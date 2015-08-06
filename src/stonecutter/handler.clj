(ns stonecutter.handler
  (:require [ring.middleware.defaults :as ring-mw]
            [ring.middleware.file :as ring-mf]
            [ring.middleware.content-type :as ring-mct]
            [ring.util.response :as r]
            [ring.adapter.jetty :as ring-jetty]
            [scenic.routes :as scenic]
            [clojure.tools.logging :as log]
            [stonecutter.view.error :as error]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.helper :as sh]
            [stonecutter.routes :as routes]
            [stonecutter.logging :as log-config]
            [stonecutter.controller.email-confirmations :as ec]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.controller.stylesheets :as stylesheets]
            [stonecutter.translation :as t]
            [stonecutter.middleware :as m]
            [stonecutter.email :as email]
            [stonecutter.db.client-seed :as client-seed]
            [stonecutter.db.storage :as s]
            [stonecutter.db.migration :as migration]
            [stonecutter.db.mongo :as mongo]
            [stonecutter.config :as config]
            [stonecutter.db.storage :as storage])
  (:gen-class))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn not-found [request]
  (-> (error/not-found-error)
      (sh/enlive-response default-context)
      (r/status 404)))

(defn err-handler [request]
  (-> (error/internal-server-error)
      (sh/enlive-response default-context)
      (r/status 500)))

(defn csrf-err-handler [req]
  (-> (error/csrf-error)
      (sh/enlive-response default-context)
      (r/status 403)))

(defn forbidden-err-handler [req]
  (-> (error/forbidden-error)
      (sh/enlive-response default-context)
      (r/status 403)))

(defn ping [request]
  (log/debug "PING REQUEST: " request)
  (-> (r/response "pong")
      (r/content-type "text/plain")))

(defn site-handlers [stores-m]
  (let [user-store (storage/get-user-store stores-m)
        client-store (storage/get-client-store stores-m)
        token-store (storage/get-token-store stores-m)]
    (->
      {:home                             user/home
       :ping                             ping
       :theme-css                        stylesheets/theme-css
       :show-registration-form           user/show-registration-form
       :register-user                    (partial user/register-user user-store token-store)
       :show-sign-in-form                user/show-sign-in-form
       :sign-in                          (partial user/sign-in user-store token-store)
       :sign-out                         user/sign-out
       :confirm-email-with-id            (partial ec/confirm-email-with-id user-store)
       :confirmation-sign-in-form        ec/show-confirm-sign-in-form
       :confirmation-sign-in             (partial ec/confirmation-sign-in user-store token-store)
       :show-profile                     (partial user/show-profile client-store user-store)
       :show-profile-created             user/show-profile-created
       :show-profile-deleted             user/show-profile-deleted
       :show-unshare-profile-card        (partial user/show-unshare-profile-card client-store user-store)
       :unshare-profile-card             (partial user/unshare-profile-card user-store)
       :show-delete-account-confirmation user/show-delete-account-confirmation
       :delete-account                   (partial user/delete-account user-store)
       :show-change-password-form        user/show-change-password-form
       :change-password                  (partial user/change-password user-store)
       :show-authorise-form              (partial oauth/show-authorise-form client-store)
       :authorise                        (partial oauth/authorise client-store user-store token-store)
       :authorise-client                 (partial oauth/authorise-client client-store user-store token-store)
       :show-authorise-failure           (partial oauth/show-authorise-failure client-store)}
      (m/wrap-handlers #(m/wrap-handle-404 % not-found) #{})
      (m/wrap-handlers #(m/wrap-handle-403 % forbidden-err-handler) #{})
      (m/wrap-handlers m/wrap-disable-caching #{:theme-css})
      (m/wrap-handlers m/wrap-signed-in #{:show-registration-form :register-user
                                          :show-sign-in-form :sign-in
                                          :sign-out
                                          :show-profile-deleted
                                          :authorise
                                          :ping
                                          :theme-css
                                          :confirm-email-with-id
                                          :confirmation-sign-in-form :confirmation-sign-in}))))

(defn api-handlers [stores-m]
  (let [user-store (storage/get-user-store stores-m)
        client-store (storage/get-client-store stores-m)
        token-store (storage/get-token-store stores-m)]
    {:validate-token (partial oauth/validate-token client-store user-store token-store)}))

(defn splitter [site api]
  (fn [request]
    (let [uri (-> request :uri)]
      (if (.startsWith uri "/api")
        (api request)
        (site request)))))

(defn handle-anti-forgery-error [req]
  (log/warn "ANTI_FORGERY_ERROR - headers: " (:headers req))
  (csrf-err-handler req))

(defn wrap-defaults-config [secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "stonecutter-session")
      (assoc-in [:security :anti-forgery] {:error-handler handle-anti-forgery-error})))

(defn create-site-app [config-m stores-m dev-mode?]
  (-> (scenic/scenic-handler routes/routes (site-handlers stores-m) not-found)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/secure? config-m)))
      m/wrap-translator
      (m/wrap-config config-m)
      (m/wrap-error-handling err-handler dev-mode?)
      (m/wrap-custom-static-resources config-m)
      ring-mct/wrap-content-type))

(defn create-api-app [config-m stores-m dev-mode?]
  (-> (scenic/scenic-handler routes/routes (api-handlers stores-m) not-found)
      (ring-mw/wrap-defaults (if (config/secure? config-m)
                               (assoc ring-mw/secure-api-defaults :proxy true)
                               ring-mw/api-defaults))
      (m/wrap-error-handling err-handler dev-mode?))) ;; TODO create json error handler

(defn create-app [config-m stores-m & {dev-mode? :dev-mode?}]
  (splitter (create-site-app config-m stores-m dev-mode?) (create-api-app config-m stores-m dev-mode?)))

(defn app [stores-m]
  (create-app (config/create-config) stores-m :dev-mode? false))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (vh/enable-template-caching!)
    (let [db (mongo/get-mongo-db (config/mongo-uri config-m))]
      (s/setup-mongo-stores! db)
      (migration/run-migrations db)
      (email/configure-email (config/email-script-path config-m))
      (client-seed/load-client-credentials-and-store-clients @s/client-store (config/client-credentials-file-path config-m))
      (ring-jetty/run-jetty (app (storage/create-mongo-stores db)) {:port (config/port config-m) :host (config/host config-m)}))))

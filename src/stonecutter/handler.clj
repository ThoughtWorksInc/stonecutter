(ns stonecutter.handler
  (:require [ring.middleware.defaults :as ring-mw]
            [ring.middleware.content-type :as ring-mct]
            [ring.util.response :as r]
            [ring.adapter.jetty :as ring-jetty]
            [scenic.routes :as scenic]
            [clojure.tools.logging :as log]
            [stonecutter.view.error :as error]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.helper :as sh]
            [stonecutter.routes :as routes]
            [stonecutter.controller.email-confirmations :as ec]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.forgotten-password :as forgotten-password]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.controller.stylesheets :as stylesheets]
            [stonecutter.translation :as t]
            [stonecutter.middleware :as m]
            [stonecutter.email :as email]
            [stonecutter.db.client-seed :as client-seed]
            [stonecutter.db.storage :as s]
            [stonecutter.db.user :as u]
            [stonecutter.db.migration :as migration]
            [stonecutter.db.mongo :as mongo]
            [stonecutter.config :as config]
            [stonecutter.admin :as admin]
            [stonecutter.db.storage :as storage]
            [stonecutter.jwt :as jwt]
            [stonecutter.util.time :as time])
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

(defn site-handlers [clock stores-m email-sender]
  (let [user-store (storage/get-user-store stores-m)
        client-store (storage/get-client-store stores-m)
        token-store (storage/get-token-store stores-m)
        confirmation-store (storage/get-confirmation-store stores-m)
        auth-code-store (storage/get-auth-code-store stores-m)
        forgotten-password-store (storage/get-forgotten-password-store stores-m)]
    (->
      {:home                                 user/home
       :ping                                 ping
       :theme-css                            stylesheets/theme-css
       :show-registration-form               user/show-registration-form
       :register-user                        (partial user/register-user user-store token-store confirmation-store email-sender)
       :show-sign-in-form                    user/show-sign-in-form
       :sign-in                              (partial user/sign-in user-store token-store)
       :sign-out                             user/sign-out
       :confirm-email-with-id                (partial ec/confirm-email-with-id user-store confirmation-store)
       :confirmation-sign-in-form            ec/show-confirm-sign-in-form
       :confirmation-sign-in                 (partial ec/confirmation-sign-in user-store token-store confirmation-store)
       :show-profile                         (partial user/show-profile client-store user-store)
       :show-profile-created                 user/show-profile-created
       :show-profile-deleted                 user/show-profile-deleted
       :show-unshare-profile-card            (partial user/show-unshare-profile-card client-store user-store)
       :unshare-profile-card                 (partial user/unshare-profile-card user-store)
       :show-delete-account-confirmation     user/show-delete-account-confirmation
       :delete-account                       (partial user/delete-account user-store)
       :show-change-password-form            user/show-change-password-form
       :change-password                      (partial user/change-password user-store)
       :show-forgotten-password-form         forgotten-password/show-forgotten-password-form
       :send-forgotten-password-email        (partial forgotten-password/forgotten-password-form-post email-sender user-store forgotten-password-store clock)
       :show-forgotten-password-confirmation forgotten-password/show-forgotten-password-confirmation
       :show-reset-password-form             (partial forgotten-password/show-reset-password-form forgotten-password-store user-store clock)
       :reset-password                       (partial forgotten-password/reset-password-form-post forgotten-password-store user-store token-store clock)
       :show-authorise-form                  (partial oauth/show-authorise-form client-store)
       :authorise                            (partial oauth/authorise auth-code-store client-store user-store token-store)
       :authorise-client                     (partial oauth/authorise-client auth-code-store client-store user-store token-store)
       :show-authorise-failure               (partial oauth/show-authorise-failure client-store)}
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
                                          :confirmation-sign-in-form :confirmation-sign-in
                                          :show-forgotten-password-form :send-forgotten-password-email
                                          :show-forgotten-password-confirmation
                                          :show-reset-password-form
                                          :reset-password}))))

(defn api-handlers [config-m stores-m id-token-generator]
  (let [auth-code-store (storage/get-auth-code-store stores-m)
        user-store (storage/get-user-store stores-m)
        client-store (storage/get-client-store stores-m)
        token-store (storage/get-token-store stores-m)]
    {:validate-token (partial oauth/validate-token config-m auth-code-store client-store user-store token-store id-token-generator)}))

(defn splitter [site api]
  (fn [request]
    (let [uri (-> request :uri)]
      (if (.startsWith uri "/api")
        (api request)
        (site request)))))

(defn handle-anti-forgery-error [req]
  (log/warn "ANTI_FORGERY_ERROR - headers: " (:headers req))
  (csrf-err-handler req))

(defn wrap-defaults-config [session-store secure?]
  (-> (if secure? (assoc ring-mw/secure-site-defaults :proxy true) ring-mw/site-defaults)
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "stonecutter-session")
      (assoc-in [:session :store] session-store)
      (assoc-in [:security :anti-forgery] {:error-handler handle-anti-forgery-error})))

(defn create-site-app [clock config-m stores-m email-sender dev-mode?]
  (-> (scenic/scenic-handler routes/routes (site-handlers clock stores-m email-sender) not-found)
      (ring-mw/wrap-defaults (wrap-defaults-config (s/get-session-store stores-m) (config/secure? config-m)))
      m/wrap-translator
      (m/wrap-config config-m)
      (m/wrap-error-handling err-handler dev-mode?)
      (m/wrap-custom-static-resources config-m)
      ring-mct/wrap-content-type))

(defn create-api-app [config-m stores-m id-token-generator dev-mode?]
  (-> (scenic/scenic-handler routes/routes (api-handlers config-m stores-m id-token-generator) not-found)
      (ring-mw/wrap-defaults (if (config/secure? config-m)
                               (assoc ring-mw/secure-api-defaults :proxy true)
                               ring-mw/api-defaults))
      (m/wrap-error-handling err-handler dev-mode?)))       ;; TODO create json error handler

(defn create-app
  ([config-m clock stores-m email-sender id-token-generator]
   (create-app config-m stores-m email-sender id-token-generator false))
  ([config-m clock stores-m email-sender id-token-generator prone-stacktraces?]
   (splitter (create-site-app clock config-m stores-m email-sender prone-stacktraces?) 
             (create-api-app config-m stores-m id-token-generator prone-stacktraces?))))

(defn -main [& args]
  (let [config-m (config/create-config)]
    (vh/enable-template-caching!)
    (let [db (mongo/get-mongo-db (config/mongo-uri config-m))
          stores-m (storage/create-mongo-stores db)
          clock (time/new-clock)
          email-sender (email/bash-sender-factory (config/email-script-path config-m))
          id-token-generator (jwt/create-generator clock (jwt/load-key-pair (config/rsa-keypair-file-path config-m))
                                                   (config/base-url config-m))
          app (create-app config-m clock stores-m email-sender id-token-generator)]
      (migration/run-migrations db)
      (admin/create-admin-user config-m (storage/get-user-store stores-m))
      (client-seed/load-client-credentials-and-store-clients (storage/get-client-store stores-m) (config/client-credentials-file-path config-m))
      (ring-jetty/run-jetty app {:port (config/port config-m) :host (config/host config-m)}))))



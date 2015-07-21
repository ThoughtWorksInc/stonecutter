(ns stonecutter.handler
  (:require [ring.middleware.defaults :as ring-mw]
            [ring.util.response :as r]
            [ring.adapter.jetty :as ring-jetty]
            [scenic.routes :as scenic]
            [clojure.tools.logging :as log]
            [stonecutter.view.error :as error]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.helper :as sh]
            [stonecutter.routes :as routes]
            [stonecutter.logging :as log-config]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.translation :as t]
            [stonecutter.middleware :as m]
            [stonecutter.db.client-seed :as client-seed]
            [stonecutter.db.storage :as s]
            [stonecutter.db.migration :as migration]
            [stonecutter.db.mongo :as mongo]
            [stonecutter.config :as config])
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

(def site-handlers
  (->
    {:home                               user/home
     :show-registration-form             user/show-registration-form
     :register-user                      user/register-user
     :show-sign-in-form                  user/show-sign-in-form
     :sign-in                            user/sign-in
     :sign-out                           user/sign-out
     :show-profile                       user/show-profile
     :show-profile-created               user/show-profile-created
     :show-profile-deleted               user/show-profile-deleted
     :show-unshare-profile-card          user/show-unshare-profile-card
     :unshare-profile-card               user/unshare-profile-card
     :show-delete-account-confirmation   user/show-delete-account-confirmation
     :delete-account                     user/delete-account
     :show-authorise-form                oauth/show-authorise-form
     :authorise                          oauth/authorise
     :authorise-client                   oauth/authorise-client
     :show-authorise-failure             oauth/show-authorise-failure}
    (m/wrap-handlers #(m/wrap-handle-404 % not-found) #{})
    (m/wrap-handlers #(m/wrap-handle-403 % forbidden-err-handler) #{})
    (m/wrap-handlers m/wrap-disable-caching #{})
    (m/wrap-handlers m/wrap-signed-in #{:show-registration-form :register-user
                                        :show-sign-in-form      :sign-in
                                        :sign-out
                                        :show-profile-deleted
                                        :authorise})))

(def api-handlers
  {:validate-token         oauth/validate-token})

(defn splitter [site api]
  (fn [request]
    (let [uri (-> request :uri)]
      (if (.startsWith uri "/api")
        (api request)
        (site request)))))

(defn handle-anti-forgery-error [req]
  (log/warn "ANTI_FORGERY_ERROR - headers: " (:headers req))
  (csrf-err-handler req))

(defn wrap-defaults-config [http-allowed?]
  (-> (if http-allowed? ring-mw/site-defaults (assoc ring-mw/secure-site-defaults :proxy true))
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "stonecutter-session")
      (assoc-in [:security :anti-forgery] {:error-handler handle-anti-forgery-error})))

(defn create-site-app [dev-mode?]
  (-> (scenic/scenic-handler routes/routes site-handlers not-found)
      (ring-mw/wrap-defaults (wrap-defaults-config (config/http-allowed?)))
      m/wrap-translator
      (m/wrap-theme (config/theme))
      (m/wrap-error-handling err-handler dev-mode?)))

(defn create-api-app [dev-mode?]
  (-> (scenic/scenic-handler routes/routes api-handlers not-found)
      (ring-mw/wrap-defaults (if (config/http-allowed?)
                               ring-mw/api-defaults
                               (assoc ring-mw/secure-api-defaults :proxy true)))
      (m/wrap-error-handling err-handler dev-mode?))) ;; TODO create json error handler

(defn create-app [& {dev-mode? :dev-mode?}]
  (splitter (create-site-app dev-mode?) (create-api-app dev-mode?)))

(def app (create-app :dev-mode? false))

(def lein-app (create-app :dev-mode? true))

(defn -main [& args]
  (log-config/init-logger!)
  (vh/enable-template-caching!)
  (let [db (mongo/get-mongo-db (config/mongo-uri))]
    (s/setup-mongo-stores! db)
    (migration/run-migrations db))
  (client-seed/load-client-credentials-and-store-clients (config/client-credentials-file-path))
  (ring-jetty/run-jetty app {:port (config/port) :host (config/host)}))

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (log-config/init-logger!)
  (vh/disable-template-caching!)
  (s/setup-in-memory-stores!)
  (client-seed/load-client-credentials-and-store-clients (config/client-credentials-file-path)))


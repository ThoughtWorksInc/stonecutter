(ns stonecutter.handler
  (:require [ring.middleware.defaults :as ring-mw]
            [ring.util.response :as r]
            [ring.adapter.jetty :as ring-jetty]
            [environ.core :as env]
            [scenic.routes :as scenic]
            [clojure.tools.logging :as log]
            [stonecutter.view.error :as error]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.db.storage :as s]
            [stonecutter.helper :as sh]
            [stonecutter.routes :as routes]
            [stonecutter.logging :as log-config]
            [stonecutter.client :as client]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.translation :as t]
            [stonecutter.middleware :as m]
            [stonecutter.db.migration :as migration]
            [stonecutter.db.mongo :as mongo])
  (:gen-class))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn not-found [request]
  (-> (sh/html-response (error/not-found-error default-context))
      (r/status 404)))

(defn err-handler [request]
  (-> (sh/html-response (error/internal-server-error default-context))
      (r/status 500)))

(defn csrf-err-handler [req]
  (-> (sh/html-response (error/csrf-error default-context))
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
     :show-delete-account-confirmation   user/show-confirm-account-confirmation
     :delete-account                     user/delete-account
     :show-authorise-form                oauth/show-authorise-form
     :authorise                          oauth/authorise
     :authorise-client                   oauth/authorise-client
     :show-authorise-failure             oauth/show-authorise-failure}
    (m/wrap-handlers m/wrap-disable-caching #{:show-sign-in-form :home})
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

(def wrap-defaults-config
  (-> ring-mw/site-defaults
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "stonecutter-session")
      (assoc-in [:security :anti-forgery] {:error-handler handle-anti-forgery-error})))

(defn create-site-app [dev-mode?]
  (-> (scenic/scenic-handler routes/routes site-handlers not-found)
      (ring-mw/wrap-defaults wrap-defaults-config)
      m/wrap-translator
      (m/wrap-error-handling err-handler dev-mode?)))

(defn create-api-app [dev-mode?]
  (-> (scenic/scenic-handler routes/routes api-handlers not-found)
      (ring-mw/wrap-defaults ring-mw/api-defaults)
      (m/wrap-error-handling err-handler dev-mode?)))                   ;; TODO create json error handler

(defn create-app [& {dev-mode? :dev-mode?}]
  (splitter (create-site-app dev-mode?) (create-api-app dev-mode?)))

(def app (create-app :dev-mode? false))

(def port (Integer. (get env/env :port "3000")))

(def host (get env/env :host "127.0.0.1"))

(defn get-docker-mongo-uri []
  (when-let [mongo-ip (get env/env :mongo-port-27017-tcp-addr)]
    (format "mongodb://%s:27017/stonecutter" mongo-ip)))

(def mongo-uri
  (or
    (get-docker-mongo-uri)
    (get env/env :mongo-uri)
    "mongodb://localhost:27017/stonecutter"))

(defn -main [& args]
  (log-config/init-logger!)
  (vh/enable-template-caching!)
  (let [db (mongo/get-mongo-db mongo-uri)]
    (s/setup-mongo-stores! db)
    (migration/run-migrations db))
  (client/load-client-credentials-and-store-clients (get env/env :client-credentials-file-path "client-credentials.yml"))
  (ring-jetty/run-jetty app {:port port :host host}))

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (log-config/init-logger!)
  (vh/disable-template-caching!)
  (s/setup-in-memory-stores!)
  (client/load-client-credentials-and-store-clients (get env/env :client-credentials-file-path "client-credentials.yml"))
  (let [user (clauth.user/register-user "user@email.com" "password")
        client-details (clauth.client/register-client "MYAPP" "myapp.com")]
    (log/info (str "TEST USER DETAILS:" user))
    (log/info (str "TEST CLIENT DETAILS:" client-details))))

(def lein-app (create-app :dev-mode? true))

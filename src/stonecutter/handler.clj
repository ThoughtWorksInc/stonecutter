(ns stonecutter.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.util.response :as r]
            [ring.adapter.jetty :refer [run-jetty]]
            [environ.core :refer [env]]
            [scenic.routes :refer [scenic-handler]]
            [clojure.tools.logging :as log]
            [clauth.endpoints :as ep]
            [stonecutter.view.error :as error]
            [stonecutter.view.view-helpers :refer [enable-template-caching! disable-template-caching!]]
            [stonecutter.db.storage :as s]
            [stonecutter.helper :refer :all]
            [stonecutter.routes :refer [routes path]]
            [stonecutter.logging :as log-config]
            [stonecutter.client :as client]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.translation :as t]
            [stonecutter.middleware :as m]
            [stonecutter.db.migration :as migration]
            [stonecutter.db.mongo :as mongo]))

(def default-context {:translator (t/translations-fn t/translation-map)})

(defn not-found [request]
  (-> (html-response (error/not-found-error default-context))
      (r/status 404)))

(defn err-handler [request]
  (-> (html-response (error/internal-server-error default-context))
      (r/status 500)))

(defn csrf-err-handler [req]
  (-> (html-response (error/csrf-error default-context))
      (r/status 403)))

(def site-handlers
  (->
    {:home                               user/home
     :show-registration-form             user/show-registration-form
     :register-user                      user/register-user
     :show-sign-in-form                  user/show-sign-in-form
     :show-authorise-form                user/show-authorise-form
     :sign-in                            user/sign-in
     :sign-out                           user/sign-out
     :show-profile                       user/show-profile
     :show-profile-created               user/show-profile-created
     :show-profile-deleted               user/show-profile-deleted
     :show-delete-account-confirmation   user/show-confirm-account-confirmation
     :delete-account                     user/delete-account
     :authorise                          oauth/authorise
     :authorise-client                   oauth/authorise-client}
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
  (-> site-defaults
      (assoc-in [:session :cookie-attrs :max-age] 3600)
      (assoc-in [:session :cookie-name] "stonecutter-session")
      (assoc-in [:security :anti-forgery] {:error-handler handle-anti-forgery-error})))

(defn create-site-app [dev-mode?]
  (-> (scenic-handler routes site-handlers not-found)
      (wrap-defaults wrap-defaults-config)
      m/wrap-translator
      (m/wrap-error-handling err-handler dev-mode?)
      ))

(defn create-api-app [dev-mode?]
  (-> (scenic-handler routes api-handlers not-found)
      (wrap-defaults api-defaults)
      (m/wrap-error-handling err-handler dev-mode?)))                   ;; TODO create json error handler

(defn create-app [& {dev-mode? :dev-mode?}]
  (splitter (create-site-app dev-mode?) (create-api-app dev-mode?)))

(def app (create-app :dev-mode? false))

(def port (Integer. (get env :port "3000")))

(defn -main [& args]
  (log-config/init-logger!)
  (enable-template-caching!)
  (let [db (mongo/get-mongo-db (get env :mongo-uri "mongodb://localhost:27017/stonecutter"))]
    (s/setup-mongo-stores! db)
    (client/delete-clients!)                                ;; TODO move this into function below
    (migration/run-migrations db))
  (client/load-client-credentials-and-store-clients (get env :client-credentials-file-path "client-credentials.yml"))
  (run-jetty app {:port port}))

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (log-config/init-logger!)
  (disable-template-caching!)
  (s/setup-in-memory-stores!)
  (client/delete-clients!)
  (client/load-client-credentials-and-store-clients (get env :client-credentials-file-path "client-credentials.yml"))
  (let [user (clauth.user/register-user "user@email.com" "password")
        client-details (clauth.client/register-client "MYAPP" "myapp.com")]
    (log/info (str "TEST USER DETAILS:" user))
    (log/info (str "TEST CLIENT DETAILS:" client-details))))

(def lein-app (create-app :dev-mode? true))

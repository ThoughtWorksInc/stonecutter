(ns stonecutter.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.util.response :as r]
            [ring.adapter.jetty :refer [run-jetty]]
            [bidi.bidi :refer [path-for]]
            [environ.core :refer [env]]
            [scenic.routes :refer [scenic-handler]]
            [clojure.tools.logging :as log]
            [stonecutter.view.register :as register]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.view.error :as error]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.view-helpers :refer [enable-template-caching! disable-template-caching!]]
            [stonecutter.translation :refer [load-translations-from-file]]
            [stonecutter.storage :as s]
            [stonecutter.utils :refer [html-response]]
            [stonecutter.routes :refer [routes path]]
            [stonecutter.logging :as log-config]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.oauth :as oauth]
            [stonecutter.translation :as t]
            [stonecutter.middleware :as m]))

(defn show-registration-form [request]
  (html-response (register/registration-form request)))

(defn show-sign-in-form [request]
  (html-response (sign-in/sign-in-form request)))

(defn show-profile [request]
  (if (get-in request [:session :user])
    (html-response (profile/profile request))
    (r/redirect (path :sign-in))))

(defn show-profile-created [request]
  (html-response (profile-created/profile-created request)))

(defn not-found [request]
  (let [context {:translator (t/translations-fn t/translation-map)}]
    (-> (html-response (error/not-found-error context))
        (r/status 404))))

(defn home [request]
  (if (get-in request [:session :user])
    (r/redirect (path :show-profile))
    (r/redirect (path :sign-in))))

(def site-handlers
  {:home                   home
   :show-registration-form show-registration-form
   :register-user          user/register-user
   :show-sign-in-form      show-sign-in-form
   :sign-in                user/sign-in
   :sign-out               user/sign-out
   :show-profile           show-profile
   :show-profile-created   show-profile-created
   :authorise              oauth/authorise})

(def api-handlers
  {:validate-token         oauth/validate-token})

(def site-handler
  (scenic-handler routes site-handlers not-found))

(def api-handler
  (scenic-handler routes api-handlers not-found))

(defn splitter [site api]
  (fn [request]
    (let [uri (-> request :uri)]
      (if (.startsWith uri "/api")
        (api request)
        (site request)))))

(def wrap-defaults-config
  (-> site-defaults
      (assoc-in [:session :cookie-attrs :max-age] 3600)))

(defn create-site-app [dev-mode?]
  (-> site-handler
      (wrap-defaults wrap-defaults-config)
      m/wrap-translator
      (m/wrap-error-handling dev-mode?)))

(defn create-api-app [dev-mode?]
  (-> api-handler
      (wrap-defaults api-defaults)
      (m/wrap-error-handling dev-mode?)))                   ;; TODO create json error handler

(defn create-app [dev-mode?]
  (splitter (create-site-app dev-mode?) (create-api-app dev-mode?)))

(def app (create-app false))

(def port (Integer. (get env :port "3000")))

(defn -main [& args]
  (log-config/init-logger!)
  (enable-template-caching!)
  (s/setup-mongo-stores! (get env :mongo-uri "mongodb://localhost:27017/stonecutter"))
  (run-jetty app {:port port}))

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (log-config/init-logger!)
  (disable-template-caching!)
  (s/setup-in-memory-stores!)
  (let [user (clauth.user/register-user "user@email.com" "password") 
        client-details (clauth.client/register-client "MYAPP" "myapp.com")]
    (log/info (str "TEST USER DETAILS:" user))
    (log/info (str "TEST CLIENT DETAILS:" client-details))))

(def lein-app (create-app true))

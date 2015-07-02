(ns stonecutter.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
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
            [stonecutter.utils :refer :all]
            [stonecutter.routes :refer [routes path]]
            [stonecutter.logging :as log-config]
            [stonecutter.controller.user :as user]
            [stonecutter.controller.oauth :as oauth]))

(def translation-map
  (load-translations-from-file "en.yml"))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))
          translation (get-in translation-map [key1 key2])]
      (when-not translation (log/warn (str "No translation found for " translation-key)))
      translation)))

(defn show-registration-form [request]
  (html-response (register/registration-form request)))

(defn show-sign-in-form [request]
  (html-response (sign-in/sign-in-form request)))

(defn show-profile [request]
  (if (get-in request [:session :user :email])
    (html-response (profile/profile request))
    (r/redirect (path :sign-in))))

(defn show-profile-created [request]
  (html-response (profile-created/profile-created request)))

(defn not-found [request]
  (let [context {:translator (translations-fn translation-map)}]
    (-> (html-response (error/not-found-error context))
        (r/status 404))))

(defn home [request]
  (if (get-in request [:session :user])
    (r/redirect (path :show-profile))
    (r/redirect (path :sign-in))))

(def handlers
  {:home                   home
   :show-registration-form show-registration-form
   :register-user          user/register-user
   :show-sign-in-form      show-sign-in-form
   :sign-in                user/sign-in
   :sign-out               user/sign-out
   :show-profile           show-profile
   :show-profile-created   show-profile-created
   :authorise              oauth/authorise
   :validate-token         oauth/validate-token})

(def app-handler
  (scenic-handler routes handlers not-found))

(defn wrap-error-handling [handler]
  (let [context {:translator (translations-fn translation-map)}]
    (fn [request]
      (try
        (handler request)
        (catch Exception e
          (log/error e)
          (-> (html-response (error/internal-server-error context)) (r/status 500)))))))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (translations-fn translation-map))
        handler)))

(def wrap-defaults-config
  (-> site-defaults
      (assoc-in [:session :cookie-attrs :max-age] 3600)))

(def app
  (-> app-handler
      (wrap-defaults wrap-defaults-config)
      wrap-translator))

(def port (Integer. (get env :port "3000")))

(defn -main [& args]
  (log-config/init-logger!)
  (enable-template-caching!)
  (s/setup-mongo-stores! (get env :mongo-uri "mongodb://localhost:27017/stonecutter"))
  (-> app wrap-error-handling (run-jetty {:port port})))

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (log-config/init-logger!)
  (disable-template-caching!)
  (s/setup-in-memory-stores!))
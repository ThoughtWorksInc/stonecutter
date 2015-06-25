(ns stonecutter.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as r]
            [ring.adapter.jetty :refer [run-jetty]]
            [bidi.bidi :refer [path-for]]
            [environ.core :refer [env]]
            [scenic.routes :refer [scenic-handler]]
            [stonecutter.view.register :as register]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.view.error :as error]
            [stonecutter.translation :refer [load-translations-from-file]]
            [stonecutter.validation :as v]
            [stonecutter.storage :as s]
            [stonecutter.routes :refer [routes path]]
            [stonecutter.logging :as log-config]
            [clojure.tools.logging :as log]))

(def translation-map
  (load-translations-from-file "en.yml"))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))
          translation (get-in translation-map [key1 key2])]
      (when-not translation (log/warn (str "No translation found for " translation-key)))
      translation)))

(defn html-response [s]
  (-> s
      r/response
      (r/content-type "text/html")))

(defn show-registration-form [request]
  (let [context {:translator (translations-fn translation-map)}]
    (html-response (register/registration-form context))))

(defn show-sign-in-form [request]
  (let [context {:translator (translations-fn translation-map)}]
    (html-response (sign-in/sign-in-form context))))

(defn register-user [request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-registration params s/is-duplicate-user?)
        context {:translator (translations-fn translation-map)
                 :errors err
                 :params params}]
    (if (empty? err)
      (do
        (s/store-user! email password)
        (html-response "You saved the user"))
      (html-response (register/registration-form context)))))

(defn show-profile [request]
  (if-let [email (get-in request [:session :user :email])]
    (html-response (str "You are signed in as " email))
    (r/redirect (path :sign-in))))

(defn sign-in [request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-sign-in params)
        context {:translator (translations-fn translation-map)
                 :params params
                 :errors err}]
    (if (empty? err)
      (if-let [user (s/retrieve-user email password)]
        (assoc (r/redirect (path :show-profile)) :session {:user user})
        (r/status (->> {:credentials :invalid}
                       (assoc context :errors)
                       sign-in/sign-in-form
                       html-response)
                400))
      (r/status (->> context sign-in/sign-in-form html-response) 400))))

(defn not-found [request]
  (let [context {:translator (translations-fn translation-map)}]
    (-> (html-response (error/not-found-error context))
        (r/status 404))))

(defn home [request]
  (if (get-in request [:session :user])
    (r/redirect (path :show-profile))
    (r/redirect (path :sign-in))))

(def handlers
  {:home home
   :show-registration-form show-registration-form
   :register-user register-user
   :show-sign-in-form show-sign-in-form
   :sign-in sign-in
   :show-profile show-profile})

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

(def app  (wrap-defaults app-handler site-defaults))

(def port (Integer. (get env :port "3000")))

(defn -main [& args]
  (log-config/init-logger!)
  (s/setup-mongo-stores! (get env :mongo-uri "mongodb://localhost:27017/stonecutter"))
  (-> app wrap-error-handling (run-jetty {:port port})))

(defn lein-ring-init
  "Function called when running app with 'lein ring server'"
  []
  (log-config/init-logger!)
  (s/setup-in-memory-stores!))

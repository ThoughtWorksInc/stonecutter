(ns stonecutter.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as r]
            [ring.adapter.jetty :refer [run-jetty]]
            [bidi.ring :refer [make-handler]]
            [environ.core :refer [env]]
            [scenic.routes :refer [scenic-handler load-routes-from-file]]
            [stonecutter.view :as view]
            [stonecutter.translation :refer [load-translations-from-file]]
            [stonecutter.validation :as v]
            [clauth.user :as user-store]))


(def translation-map
  (load-translations-from-file "en.yml"))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))]
      (get-in translation-map [key1 key2]))))

(defn html-response [s]
  (-> s
      r/response
      (r/content-type "text/html")))

(defn show-registration-form [r]
  (html-response (view/registration-form (translations-fn translation-map) nil)))

(defn register-user [r]
    (let [params (:params r)
          email (:email params)
          password (:password params)
          err (v/validate-registration params)]
      (if-not err
        (do 
          (-> (user-store/new-user email password)
              user-store/store-user)
          (html-response "You saved the user")) 
        (html-response (view/registration-form (translations-fn translation-map) err))) 
      ))

(defn not-found [r]
  (html-response "These are not the droids you are looking for.."))

(def handlers 
  {:home (fn [r] (html-response "Hello World"))
   :show-registration-form show-registration-form
   :register-user register-user})

(def routes 
  (scenic-handler (load-routes-from-file "routes.txt") handlers not-found))

(def app
  (wrap-defaults routes site-defaults))

(def port (Integer. (get env :port "3000")))

(defn -main [& args]   
  (run-jetty app {:port port}))

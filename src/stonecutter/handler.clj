(ns stonecutter.handler
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as r]
            [ring.adapter.jetty :refer [run-jetty]]
            [bidi.ring :refer [make-handler]]
            [stonecutter.view :as view]
            ))

(defn html-response [s]
  (-> s
      r/response
      (r/content-type "text/html")))

(defn show-registration-form [r]
  (html-response (view/registration-form)))

(def routes 
  (make-handler ["/" {"register" show-registration-form}]))

(def app
  (wrap-defaults routes site-defaults))

(defn -main [& args]   
  (run-jetty app {:port 3000})) 


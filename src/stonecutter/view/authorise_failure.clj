(ns stonecutter.view.authorise-failure
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(defn set-redirect-to-client-home-link [params enlive-m]
  (html/at enlive-m
           [:.clj--redirect-to-client-home__link] (html/set-attr :href (:callback-uri-with-error params))))

(defn set-client-name [client-name enlive-m]
  (html/at enlive-m
           [:.clj--client-name] (html/content client-name)))

(defn show-authorise-failure [request]
  (let [client-name (get-in request [:context :client-name])
        params (:params request)]
    (->> (vh/load-template-with-lang "public/authorise-failure.html" request)
         (set-redirect-to-client-home-link params)
         (set-client-name client-name)
         vh/remove-work-in-progress)))

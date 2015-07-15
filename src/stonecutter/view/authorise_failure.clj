(ns stonecutter.view.authorise-failure
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(defn set-redirect-to-client-home-link [params enlive-m]
  (html/at enlive-m
           [:.clj--redirect-to-client-home__link] (html/set-attr :href (:callback-uri-with-error params))))

(defn set-client-app-name [session enlive-m]
  (html/at enlive-m
           [:.clj--app-name] (html/content (:client-name session))))

(defn show-authorise-failure [request]
  (let [session (:session request)
        params (:params request)]
    (->> (vh/load-template "public/authorise-failure.html")
         vh/remove-work-in-progress
         (set-redirect-to-client-home-link params)
         (set-client-app-name session))))

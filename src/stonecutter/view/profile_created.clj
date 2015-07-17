(ns stonecutter.view.profile-created
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(def from-app-translation-tag "content:profile-created/action-button-from-app")
(def default-translation-tag "content:profile-created/action-button-default")

(defn set-button [enlive-m transformation]
  (html/at enlive-m
           [:.clj--profile-created-next__button]
           transformation))

(defn set-next-link
  [next-url enlive-m]
  (set-button enlive-m (html/set-attr :href next-url)))

(defn set-button-text
  [from-app enlive-m]
  (set-button enlive-m (html/set-attr :data-l8n (if from-app
                                                  from-app-translation-tag
                                                  default-translation-tag))))
(defn get-next-url [from-app request]
  (if from-app (get-in request [:session :return-to]) (r/path :show-profile)))

(defn profile-created [request]
  (let [from-app (get-in request [:params :from-app])
        next-url (get-next-url from-app request)]
    (->> (vh/load-template "public/profile-created.html")
         (set-next-link next-url)
         (set-button-text from-app)
         vh/remove-work-in-progress)))

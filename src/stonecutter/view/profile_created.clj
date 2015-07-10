(ns stonecutter.view.profile-created
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(defn set-next-link [enlive-m]
  (html/at enlive-m
           [:.clj--profile-created-next__button] (html/set-attr :href (r/path :show-profile))))

(defn profile-created [request]
  (let [context (:context request)
        translator (:translator context)]
    (->> (vh/load-template "public/profile-created.html")
         set-next-link
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

(ns stonecutter.view.profile-created
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(defn set-next-link
  [from-app return-to enlive-m]
  (html/at enlive-m
           [:.clj--profile-created-next__button]
           (html/set-attr :href (if from-app return-to (r/path :show-profile)))))

(defn profile-created [request]
  (let [context (:context request)
        translator (:translator context)
        from-app (get-in request [:params :from-app])]
    (->> (vh/load-template "public/profile-created.html")
         (set-next-link from-app (get-in request [:session :return-to]))
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

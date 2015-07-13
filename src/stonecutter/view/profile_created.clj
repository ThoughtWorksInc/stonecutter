(ns stonecutter.view.profile-created
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(defn set-next-link
  [from-app enlive-m]
  (html/at enlive-m
           [:.clj--profile-created-next__button]
           (html/set-attr :href (r/path (if from-app
                                          :show-authorise-form
                                          :show-profile)))))

(defn profile-created [request]
  (let [context (:context request)
        translator (:translator context)
        from-app (get-in request [:params :from-app])]
    (->> (vh/load-template "public/profile-created.html")
         (set-next-link from-app)
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

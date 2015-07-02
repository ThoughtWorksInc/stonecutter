(ns stonecutter.view.profile-created
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]))

(defn profile-created [request]
  (let [context (:context request)
        translator (:translator context)]
    (->> (vh/load-template "public/profile-created.html")
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))


(ns stonecutter.view.authorise
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn authorise-form [request]
  (let [context (:context request)
        translator (:translator context)]
    (->> (vh/load-template "public/authorise.html")
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

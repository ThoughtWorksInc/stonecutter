(ns stonecutter.view.profile 
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]))

(defn add-email [context enlive-m]
  (let [email (get-in context [:session :user :email])]
      (-> enlive-m
          (html/at [:.clj--card--name] (html/content email)))))

(defn profile [context]
    (->> (vh/load-template "public/profile.html")
         (add-email context)
         html/emit*
         (apply str)))

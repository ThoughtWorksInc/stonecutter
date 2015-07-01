(ns stonecutter.view.profile 
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn add-username [context enlive-m]
  (let [email (get-in context [:session :user :email])]
    (-> enlive-m
        (html/at [:.clj--card--name] (html/content email)))))

(defn add-sign-out-link [enlive-m]
  (html/at enlive-m 
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn pri [v] (prn v) v)

(defn profile [request]
  (let [context (:context request)
        translator (:translator context)]
    (->> (vh/load-template "public/profile.html")
         (add-username request)
         add-sign-out-link
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

(ns stonecutter.view.profile
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn add-username [request enlive-m]
  (let [email (get-in request [:session :user-login])]
    (html/at enlive-m
             [:.clj--card--name] (html/content email))))

(def library-template (vh/load-template "public/library.html"))

(def application-list-item (first (html/select library-template [:.clj--authorised-app__list-item])))

(def empty-application-list-item (first (html/select library-template [:.clj--authorised-app__list-item--empty])))

(defn application-list-items [authorised-clients]
  (html/at application-list-item
           [:.clj--authorised-app__list-item]
           (html/clone-for [client authorised-clients]
                           [:.clj--authorised-app__title] (html/content (:name client)))))

(defn add-application-list [request enlive-m]
  (let [authorised-clients (get-in request [:context :authorised-clients])]
    (if-not (empty? authorised-clients)
      (html/at enlive-m [:.clj--app__list] (html/content (application-list-items authorised-clients)))
      (html/at enlive-m [:.clj--app__list] (html/content empty-application-list-item)))))

(defn add-sign-out-link [enlive-m]
  (html/at enlive-m
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn profile [request]
  (->> (vh/load-template "public/profile.html")
       (add-username request)
       (add-application-list request)
       add-sign-out-link
       vh/remove-work-in-progress))

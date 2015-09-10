(ns stonecutter.view.user-list
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(def library-m (vh/load-template "public/library.html"))

(def empty-user-list-item (first (html/select library-m [:.clj--user-list__item__empty])))

(def user-with-confirmed-email-list-item
  (first (html/select library-m [:.clj-library--user-list__user-with-confirmed-email :.clj--user-item])))

(def unconfirmed-email-icon (first (html/select library-m [:.clj--user-item__email-unconfirmed])))

(def user-displaying-both-confirmed-and-unconfirmed-email
  (html/at user-with-confirmed-email-list-item
           [:.clj--user-item__email-address] (html/prepend unconfirmed-email-icon)))

(defn user-list-items [users]
  (html/at user-displaying-both-confirmed-and-unconfirmed-email
           [:.clj--user-item]
           (html/clone-for [user users]
                           [:.clj--user-item__email-confirmed] (when (:confirmed? user) identity)
                           [:.clj--user-item__email-unconfirmed] (when-not (:confirmed? user) identity)
                           [:.clj--user-item__email-address__text] (html/content (:login user)))))

(defn add-user-list [enlive-m users]
  (if-not (empty? users)
    (html/at enlive-m [:.clj--user-list] (html/content (user-list-items users)))
    (html/at enlive-m [:.clj--user-list] (html/content empty-user-list-item))))

(defn set-sign-out-link [enlive-m]
  (html/at enlive-m
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn user-list [request]
  (let [users (get-in request [:context :users])]
    (-> (vh/load-template "public/user-list.html")
        (add-user-list users)
        set-sign-out-link
        vh/remove-work-in-progress)))

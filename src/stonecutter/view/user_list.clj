(ns stonecutter.view.user-list
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(defn user-with-confirmed-email-list-item [library-m] (first (html/select library-m [:.clj-library--user-list__user-with-confirmed-email])))

(defn user-with-unconfirmed-email-list-item [library-m] (first (html/select library-m [:.clj-library--user-list__user-with_unconfirmed-email])))

(defn empty-user-list-item [library-m] (first (html/select library-m [:.clj--user-list__item__empty])))

(defn user-list-items [users library-m]
  (html/at (user-with-confirmed-email-list-item library-m)
           [:.clj--user-item]
           (html/clone-for [user users]
                           [:.clj--user-item__email-address__text] (html/content (:login user)))))

(defn add-user-list [enlive-m users library-m]
  (if-not (empty? users)
    (html/at enlive-m [:.clj--user-list] (html/content (user-list-items users library-m)))
    (html/at enlive-m [:.clj--user-list] (html/content (empty-user-list-item library-m)))))

(defn set-sign-out-link [enlive-m]
  (html/at enlive-m
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn user-list [request]
  (let [library-m (vh/load-template "public/library.html")
        users (get-in request [:context :users])]
    (-> (vh/load-template "public/user-list.html")
        (add-user-list users library-m)
        set-sign-out-link
        vh/remove-work-in-progress)))
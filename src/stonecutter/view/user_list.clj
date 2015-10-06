(ns stonecutter.view.user-list
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]
            [stonecutter.config :as config]))

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
                           [:.clj--user-item__toggle] (if (= (:role user) (:trusted config/roles))
                                                        (html/set-attr :checked "checked")
                                                        (html/remove-attr :checked))
                           [:.clj--user-item__label] (html/set-attr :for (:login user))
                           [:.clj--user-item__toggle] (html/set-attr :id (:login user))
                           [:.clj--user-item__email-input] (html/set-attr :value (:login user))
                           [:.clj--user-item__email-address__text] (html/content (:login user))
                           [:.clj--user-item__full-name] (html/content (str (:first-name user) " " (:last-name user))))))

(defn add-user-list [enlive-m users]
  (if-not (empty? users)
    (html/at enlive-m [:.clj--user-list] (html/content (user-list-items users)))
    (html/at enlive-m [:.clj--user-list] (html/content empty-user-list-item))))

(defn not-an-admin? [user]
  (not (= (:admin config/roles) (:role user))))

(defn set-flash-message [enlive-m request]
  (let [translation-key (get-in request [:flash :translation-key])
        updated-account-email (get-in request [:flash :updated-account-email])
        enlive-m-with-user-login (html/at enlive-m [:.clj--flash-message-login] (html/content updated-account-email))]
    (case translation-key
      :user-trusted (html/at enlive-m-with-user-login [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/user-trusted"))
      :user-untrusted (html/at enlive-m-with-user-login [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/user-untrusted"))
      (vh/remove-element enlive-m [:.clj--flash-message-container]))))


(defn user-list [request]
  (let [users (get-in request [:context :users])
        non-admin-users (filterv not-an-admin? users)]
    (-> (vh/load-template "public/user-list.html")
        (add-user-list non-admin-users)
        (set-flash-message request)
        vh/set-sign-out-link
        vh/set-apps-list-link
        vh/set-user-list-link
        vh/add-anti-forgery
        (#(vh/add-script "../js/main.js" %))
        vh/remove-work-in-progress)))

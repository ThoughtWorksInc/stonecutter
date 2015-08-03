(ns stonecutter.view.profile
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.toggles :as toggles]
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
                           [:.clj--client-name] (html/content (:name client))
                           [:.clj--app-item__unshare-link] (html/set-attr :href (str (r/path :show-unshare-profile-card)
                                                                                     "?client_id="
                                                                                     (:client-id client))))))

(defn add-application-list [request enlive-m]
  (let [authorised-clients (get-in request [:context :authorised-clients])]
    (if-not (empty? authorised-clients)
      (html/at enlive-m [:.clj--app__list] (html/content (application-list-items authorised-clients)))
      (html/at enlive-m [:.clj--app__list] (html/content empty-application-list-item)))))

(defn set-sign-out-link [enlive-m]
  (html/at enlive-m
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn set-change-password-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password__link] (html/set-attr :href (r/path :show-change-password-form))))

(defn set-delete-account-link [enlive-m]
  (html/at enlive-m
           [:.clj--delete-account__link] (html/set-attr :href (r/path :show-delete-account-confirmation))))

(defn display-email-confirmation-status [request enlive-m]
    (let [confirmed? (get-in request [:context :confirmed?])]
      (if confirmed?
        (html/at enlive-m [:.clj--email-not-confirmed-message] nil)
        (html/at enlive-m [:.clj--email-confirmed-message] nil))))

(defn add-flash-message [request enlive-m]
  (if (= (:flash request) :password-changed)
    enlive-m
    (vh/remove-element enlive-m [:.clj--flash-message-container])))

(defn profile [request]
  (->> (vh/load-template "public/profile.html")
       (add-flash-message request)
       (display-email-confirmation-status request)
       (add-username request)
       (add-application-list request)
       set-sign-out-link
       set-change-password-link
       set-delete-account-link
       vh/remove-work-in-progress))

(ns stonecutter.view.profile
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.config :as config]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.session :as session]))

(defn add-profile-card [enlive-m request]
  (let [email (get-in request [:context :user-login])
        first-name (get-in request [:context :user-first-name])
        last-name (get-in request [:context :user-last-name])]
    (html/at enlive-m
             [:.clj--card-name] (html/content (str first-name " " last-name))
             [:.clj--card-email] (html/content email))))

(defn application-list-item [library-m] (first (html/select library-m [:.clj--authorised-app__list-item])))

(defn empty-application-list-item [library-m] (first (html/select library-m [:.clj--authorised-app__list-item--empty])))

(defn application-list-items [authorised-clients library-m]
  (html/at (application-list-item library-m)
           [:.clj--authorised-app__list-item]
           (html/clone-for [client authorised-clients]
                           [:.clj--client-name] (html/content (:name client))
                           [:.clj--app-item__unshare-link] (html/set-attr :href (str (r/path :show-unshare-profile-card)
                                                                                     "?client_id="
                                                                                     (:client-id client))))))

(defn add-application-list [enlive-m request library-m]
  (let [authorised-clients (get-in request [:context :authorised-clients])]
    (if-not (empty? authorised-clients)
      (html/at enlive-m [:.clj--app__list] (html/content (application-list-items authorised-clients library-m)))
      (html/at enlive-m [:.clj--app__list] (html/content (empty-application-list-item library-m))))))

(defn set-sign-out-link [enlive-m]
  (html/at enlive-m
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn set-change-password-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password__link] (html/set-attr :href (r/path :show-change-password-form))))

(defn set-delete-account-link [enlive-m]
  (html/at enlive-m
           [:.clj--delete-account__link] (html/set-attr :href (r/path :show-delete-account-confirmation))))

(defn hide-admin-span [enlive-m request]
  (let [role (get-in request [:context :role])]
    (html/at enlive-m [:.clj--admin__span]
             (when (= role (:admin config/roles)) identity))))

(defn diplay-email-unconfirmed-message [enlive-m request]
  (if (= false (get-in request [:context :confirmed?]))
    (let [email (session/request->user-login request)]
      (html/at enlive-m [:.clj--unconfirmed-email] (html/content email)))
    (vh/remove-element enlive-m [:.clj--unconfirmed-email-message-container])))

(defn set-flash-message [enlive-m request]
  (case (:flash request)
    :password-changed (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/password-changed"))
    :email-confirmed (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/email-confirmed"))
    :confirmation-email-sent (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/confirmation-email-sent"))
    :email-already-confirmed (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/email-already-confirmed"))
    (vh/remove-element enlive-m [:.clj--flash-message-container])))

(defn profile [request]
  (let [library-m (vh/load-template "public/library.html")]
    (-> (vh/load-template "public/profile.html")
        (set-flash-message request)
        (diplay-email-unconfirmed-message request)
        (add-profile-card request)
        (add-application-list request library-m)
        set-sign-out-link
        set-change-password-link
        set-delete-account-link
        (hide-admin-span request)
        vh/remove-work-in-progress
        vh/add-anti-forgery)))

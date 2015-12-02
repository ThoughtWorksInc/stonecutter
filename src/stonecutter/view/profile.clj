(ns stonecutter.view.profile
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.config :as config]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.session :as session]))

(defn add-profile-card [enlive-m request]
  (let [email (get-in request [:context :user-login])
        first-name (get-in request [:context :user-first-name])
        last-name (get-in request [:context :user-last-name])
        profile-picture (get-in request [:context :user-profile-picture])]
    (html/at enlive-m
             [:.clj--card-name] (html/content (str first-name " " last-name))
             [:.clj--card-email] (html/content email)
             [:.clj--card-image :img] (html/set-attr :src profile-picture))))

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

(defn set-change-password-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password__link] (html/set-attr :href (r/path :show-change-password-form))))

(defn set-delete-account-link [enlive-m]
  (html/at enlive-m
           [:.clj--delete-account__link] (html/set-attr :href (r/path :show-delete-account-confirmation))))

(defn set-change-email-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-email__link] (html/set-attr :href (r/path :show-change-email-form))))

(defn hide-admin-span [enlive-m request]
  (let [role (get-in request [:session :role])]
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
    :email-changed (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/email-changed"))
    :confirmation-email-sent (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/confirmation-email-sent"))
    :email-already-confirmed (html/at enlive-m [:.clj--flash-message-text] (html/set-attr :data-l8n "content:flash/email-already-confirmed"))
    (vh/remove-element enlive-m [:.clj--flash-message-container])))

(defn profile [request]
  (let [library-m (vh/load-template "public/library.html")]
    (-> (vh/load-template-with-lang "public/profile.html" request)
        (set-flash-message request)
        (diplay-email-unconfirmed-message request)
        (add-profile-card request)
        (add-application-list request library-m)
        (vh/display-admin-navigation-links request library-m)
        set-change-password-link
        set-delete-account-link
        set-change-email-link
        (hide-admin-span request)
        vh/remove-work-in-progress
        vh/add-anti-forgery)))

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

(defn set-change-profile-details-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-profile-details__link] (html/set-attr :href (r/path :show-change-profile-form))))

(defn set-update-profile-image-link [enlive-m]
  (html/at enlive-m
           [:.clj--update-profile-profile__link] (html/set-attr :action (r/path :update-profile-image))
           [:.clj--card-photo-upload] (html/set-attr :action (r/path :update-profile-image))))

(defn set-download-vcard-link [enlive-m]
  (html/at enlive-m
           [:.clj--download-vcard__link] (html/set-attr :action (r/path :download-vcard))))

(defn hide-admin-span [enlive-m request]
  (let [role (get-in request [:session :role])]
    (html/at enlive-m [:.clj--admin__span]
             (when (= role (:admin config/roles)) identity))))


(defn diplay-email-unconfirmed-message [enlive-m request]
  (if (= false (get-in request [:context :confirmed?]))
    (let [email (session/request->user-login request)]
      (html/at enlive-m [:.clj--unconfirmed-email] (html/content email)))
    (vh/remove-element enlive-m [:.clj--unconfirmed-email-message-container])))

(defn set-translation [enlive-m text-class translation]
  (html/at enlive-m [text-class] (html/set-attr :data-l8n translation)))

(defn set-flash-message [enlive-m request]
  (case (:flash request)
       :password-changed        (set-translation enlive-m :.clj--flash-message-text "content:flash/password-changed")
       :email-confirmed         (set-translation enlive-m :.clj--flash-message-text "content:flash/email-confirmed")
       :email-changed           (set-translation enlive-m :.clj--flash-message-text "content:flash/email-changed")
       :confirmation-email-sent (set-translation enlive-m :.clj--flash-message-text "content:flash/confirmation-email-sent")
       :email-already-confirmed (set-translation enlive-m :.clj--flash-message-text "content:flash/email-already-confirmed")
       :profile-details-changed (set-translation enlive-m :.clj--flash-message-text "content:flash/profile-details-changed")
       (vh/remove-element enlive-m [:.clj--flash-message-container])))

(defn set-image-error-message [enlive-m request]
  (case (:flash request)
       :not-image             (set-translation enlive-m :.clj--profile-image-error-text "content:upload-profile-picture/file-not-image-validation-message")
       :too-large             (set-translation enlive-m :.clj--profile-image-error-text "content:upload-profile-picture/file-too-large-validation-message")
       :unsupported-extension (set-translation enlive-m :.clj--profile-image-error-text "content:upload-profile-picture/file-type-not-supported-validation-message")
       (html/at enlive-m [:.clj--profile-image-error-container] (html/set-attr :hidden "hidden"))))

(defn profile [request]
  (let [library-m (vh/load-template "public/library.html")]
    (-> (vh/load-template-with-lang "public/profile.html" request)
        (set-flash-message request)
        (set-image-error-message request)
        (diplay-email-unconfirmed-message request)
        (add-profile-card request)
        (add-application-list request library-m)
        (vh/display-admin-navigation-links request library-m)
        set-change-password-link
        set-delete-account-link
        set-change-email-link
        set-change-profile-details-link
        set-update-profile-image-link
        set-download-vcard-link
        (hide-admin-span request)
        vh/remove-work-in-progress
        vh/add-anti-forgery
        (vh/add-script "js/main.js"))))

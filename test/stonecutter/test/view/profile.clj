(ns stonecutter.test.view.profile
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.profile :refer [profile]]
            [stonecutter.helper :as helper]
            [stonecutter.config :as config]))

(fact "profile should return some html"
      (let [page (-> (th/create-request) profile)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) profile)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        page => (th/has-attr? [:.clj--sign-out__link] :href (r/path :sign-out))))

(fact "profile link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        page => (th/has-attr? [:.clj--profile__link] :href (r/path :show-profile))))

(fact "change password link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        page => (th/has-attr? [:.clj--change-password__link] :href (r/path :show-change-password-form))))

(fact "change email link should go to the correct endpoint"
      (let [page (-> (th/create-request) profile)]
        page => (th/has-attr? [:.clj--change-email__link] :href (r/path :show-change-email-form))))

(fact "delete account link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        page => (th/has-attr? [:.clj--delete-account__link] :href (r/path :show-delete-account-confirmation))))

(fact "update profile picture should post to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        (html/select page [:.clj--update-profile-profile__link]) => (th/has-form-action? (r/path :update-profile-image))
        (html/select page [:.clj--card-photo-upload]) => (th/has-form-action? (r/path :update-profile-image))))

(fact
  (let [translator (t/translations-fn t/translation-map)
        request (-> (th/create-request translator)
                    (assoc-in [:context :authorised-clients] [{:name "Bloc Party" :client-id "some-client-id"}]))]
    (th/test-translations "profile" profile request)))

(fact "csrf token should be inserted"
      (let [page (-> (th/create-request) (assoc-in [:context :confirmed?] false) profile)]
        page => (th/element-exists? [:input#__anti-forgery-token])))

(facts "about displaying navigation bar"
       (fact "apps and users links are not displayed if the user role is not admin"
             (let [page (-> (th/create-request)
                            profile)]
               (-> page (html/select [:.clj--apps-list__link])) => empty?
               (-> page (html/select [:.clj--users-list__link])) => empty?))

       (fact "apps and users links are displayed and go to the correct endpoint if the user role is admin"
             (let [page (-> (th/create-request)
                            (assoc-in [:session :role] (:admin config/roles))
                            profile)]
               (-> page (html/select [:.clj--apps-list__link])) =not=> empty?
               (-> page (html/select [:.clj--user-list__link])) =not=> empty?
               page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))
               page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list)))))

(facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request)
                            profile)]
               (-> page (html/select [:.clj--flash-message-container])) => empty?))

       (tabular
         (fact "appropriate flash message is displayed on page when a flash key is included in the request"
               (let [page (-> (th/create-request) (assoc :flash ?flash-key) profile)]
                 (-> page (html/select [:.clj--flash-message-container])) =not=> empty?
                 (-> page (html/select [:.clj--flash-message-text]) first :attrs :data-l8n)
                 => ?translation-key))

         ?flash-key                 ?translation-key
         :email-changed             "content:flash/email-changed"
         :password-changed          "content:flash/password-changed"
         :email-confirmed           "content:flash/email-confirmed"
         :confirmation-email-sent   "content:flash/confirmation-email-sent"
         :email-already-confirmed   "content:flash/email-already-confirmed"))

(facts "about image upload errors"
       (fact "no errors are displayed by default"
             (let [page (-> (th/create-request)
                            profile)]
               page => (th/has-attr? [:.clj--profile-image-error-container] :hidden "hidden")))
       (tabular
         (fact "appropriate error message is displayed on page when an image error key is included in the request"
               (let [page (-> (th/create-request) (assoc :flash ?error-key) profile)]
                 (-> page (html/select [:.clj--profile-image-error-container])) =not=> empty?
                 (-> page (html/select [:.clj--profile-image-error-text]) first :attrs :data-l8n)
                 => ?translation-key))

         ?error-key                 ?translation-key
         :not-image                 "content:image-error/not-image"
         :too-large                 "content:image-error/too-large"
         :unsupported-extension     "content:image-error/unsupported-filetype"))

(facts "about displaying email confirmation status"
       (fact "the unconfirmed email message is removed when :confirmed? context is not false"
             (let [page (-> (th/create-request) profile)]
               (-> page (html/select [:.clj--unconfirmed-email-message-container])) => empty?))

       (fact "accounts with unconfirmed email addresses display unconfirmed message as if it were a flash message"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :confirmed?] false)
                            (assoc-in [:session :user-login] "valid@web.co.uk")
                            profile)]
               (-> page (html/select [:.clj--unconfirmed-email-message-container])) =not=> empty?
               (-> page (html/select [:.clj--unconfirmed-email]) first html/text) => "valid@web.co.uk")))

(facts "about displaying authorised clients"
       (fact "names of authorised clients are displayed"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :authorised-clients] [{:name "Bloc Party"}
                                                                      {:name "Tabletennis Party"}])
                            profile)]
               (-> page
                   (html/select [:.func--app__list])
                   first
                   html/text) => (contains #"Bloc Party[\s\S]+Tabletennis Party")))

       (fact "unshare card button links include the client_id query param"
             (let [client-id "bloc_party_client-id"
                   page (-> (th/create-request)
                            (assoc-in [:context :authorised-clients] [{:name "Bloc Party" :client-id client-id}])
                            profile)]
               page => (th/has-attr? [:.clj--app-item__unshare-link]
                                     :href (str (r/path :show-unshare-profile-card) "?client_id=" client-id))))

       (fact "empty application-list item is used when there are no authorised clients"
             (let [page (-> (th/create-request)
                            profile)]
               (html/select page [:.clj--authorised-app__list-item--empty]) =not=> empty?
               (html/select page [:.clj--authorised-app__list-item]) => empty?)))

(facts "about displaying profile card"
       (let [page (-> (th/create-request)
                      (assoc-in [:context :user-login] "valid@web.co.uk")
                      (assoc-in [:context :user-first-name] "Frank")
                      (assoc-in [:context :user-last-name] "Lasty")
                      (assoc-in [:context :user-profile-picture] "/images/temp-avatar-300x300.png")
                      profile)]
         (fact "it should display email address"
               (-> page (html/select [:.clj--card-email]) first html/text) => "valid@web.co.uk")

         (fact "it should display full name"
               (-> page (html/select [:.clj--card-name]) first html/text) => "Frank Lasty")

         (fact "it should display profile picture"
               (-> page (html/select [:.clj--card-image :img]) first :attrs :src) => "/images/temp-avatar-300x300.png")))


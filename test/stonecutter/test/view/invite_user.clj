(ns stonecutter.test.view.invite-user
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.invite-user :refer [invite-user]]
            [stonecutter.translation :as t]
            [stonecutter.config :as config]
            [stonecutter.view.invite-user :as invite-user]))

(fact "invite user should return some html"
      (let [page (-> (th/create-request) invite-user)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) invite-user)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) invite-user)]
        page => (th/has-attr? [:.clj--sign-out__link] :href (r/path :sign-out))))

(fact "profile link should go to correct endpoint"
      (let [page (-> (th/create-request) invite-user)]
        page => (th/has-attr? [:.clj--profile__link] :href (r/path :show-profile))))

(fact "user list link should go to correct endpoint"
      (let [page (-> (th/create-request) invite-user)]
        page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list))))

(fact "apps list link should go to correct endpoint"
      (let [page (-> (th/create-request) invite-user)]
        page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))))

(fact "invite link should go to correct endpoint"
      (let [page (-> (th/create-request) invite-user)]
        page => (th/has-attr? [:.clj--invite__link] :href (r/path :show-invite))))

(fact "there are no missing translations"
  (let [translator (t/translations-fn t/translation-map)
        request (th/create-request translator)]
    (th/test-translations "invite-user" invite-user request)))

(fact "no flash messages are displayed by default"
      (let [page (-> (th/create-request) invite-user)]
        (-> page (html/select [:.clj--flash-message-text])) => empty?))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request nil nil {}) invite-user/invite-user)]
        page => (th/has-form-action? (r/path :send-invite))
        page => (th/has-form-method? "post")))

(future-facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request) invite-user)]
               (-> page (html/select [:.func--flash-message-add-container])) => empty?
               (-> page (html/select [:.func--flash-message-delete-container])) => empty?))

       (fact "successful add flash message is displayed on page when a flash key is included in the request"
             (let [page (-> (th/create-request)
                            (assoc-in [:flash :added-app-name] "new-client-name")
                            invite-user)]
               (-> page (html/select [:.clj--flash-message-add-container])) =not=> empty?
               (-> page (html/select [:.clj--flash-message-delete-container])) => empty?
               (-> page (html/select [:.clj--new-app-name]) first html/text) => (contains "new-client-name")))

       (fact "successful delete flash message is displayed on page when a flash key is included in the request"
             (let [page (-> (th/create-request)
                            (assoc-in [:flash :deleted-app-name] "client-name")
                            invite-user)]

               (-> page (html/select [:.clj--flash-message-delete-container])) =not=> empty?
               (-> page (html/select [:.clj--flash-message-add-container])) => empty?
               (-> page (html/select [:.clj--deleted-app-name]) first html/text) => (contains "client-name"))))

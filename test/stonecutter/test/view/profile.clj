(ns stonecutter.test.view.profile
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.toggles :as toggles]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.profile :refer [profile]]
            [stonecutter.helper :as helper]))

(fact "profile should return some html"
      (let [page (-> (th/create-request) profile)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) profile)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        (-> page (html/select [:.clj--sign-out__link])
            first :attrs :href) => (r/path :sign-out)))

(fact "change password link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        (-> page (html/select [:.clj--change-password__link])
            first :attrs :href) => (r/path :show-change-password-form)))

(fact "delete account link should go to correct endpoint"
      (let [page (-> (th/create-request) profile)]
        (-> page (html/select [:.clj--delete-account__link])
            first :attrs :href) => (r/path :show-delete-account-confirmation)))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator)
                     (assoc-in [:context :authorised-clients] [{:name "Bloc Party" :client-id "some-client-id"}])
                     profile (helper/enlive-response {:translator translator}) :body)]
        page => th/no-untranslated-strings))

(facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request) profile)]
               (-> page (html/select [:.clj--flash-message-container])) => empty?))

       (fact "password-changed flash message is displayed on page if it is in the flash of request"
             (let [page (-> (th/create-request) (assoc :flash :password-changed) profile)]
               (-> page (html/select [:.clj--flash-message-container])) =not=> empty?)))

(when (= toggles/story-25 :activated)
 (facts "about displaying email confirmation status"
       (fact "accounts with unconfirmed email addresses display unconfirmed message and don't display confirmed message"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :confirmed?] false)
                            profile)]
               (-> page (html/select [:.clj--email-not-confirmed-message])) =not=> empty?   
               (-> page (html/select [:.clj--email-confirmed-message])) => empty?))))

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
               (-> page (html/select [:.clj--app-item__unshare-link])
                   first
                   :attrs
                   :href) => (str (r/path :show-unshare-profile-card) "?client_id=" client-id)))

       (fact "empty application-list item is used when there are no authorised clients"
             (let [page (-> (th/create-request)
                            profile)]
                (html/select page [:.clj--authorised-app__list-item--empty]) =not=> empty?
                (html/select page [:.clj--authorised-app__list-item]) => empty?)))

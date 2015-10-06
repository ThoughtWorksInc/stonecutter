(ns stonecutter.test.view.user-list
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.user-list :refer [user-list]]
            [stonecutter.translation :as t]
            [stonecutter.config :as config]))


(fact "user-list should return some html"
      (let [page (-> (th/create-request) user-list)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) user-list)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) user-list)]
        page => (th/has-attr? [:.clj--sign-out__link] :href (r/path :sign-out))))

(fact "user list link should go to correct endpoint"
      (let [page (-> (th/create-request) user-list)]
        page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list))))

(fact "apps list link should go to correct endpoint"
      (let [page (-> (th/create-request) user-list)]
        page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))))

(facts "about the list of users"
       (fact "users are displayed along with their email confirmation status"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [{:login "confirmed@email.com" :confirmed? true :role (:untrusted config/roles)}
                                                         {:login "unconfirmed@email.com" :confirmed? false :role (:untrusted config/roles)}])
                            user-list)]
               (-> page (html/select [:.clj--user-item]))
               => (just [(th/element-exists? [:.clj--user-item__email-confirmed])
                         (th/element-exists? [:.clj--user-item__email-unconfirmed])])

               (-> page (html/select [:.clj--user-item]))
               => (just [(th/text-is? [:.clj--user-item__email-address__text] "confirmed@email.com")
                         (th/text-is? [:.clj--user-item__email-address__text] "unconfirmed@email.com")])))

       (fact "users are displayed along with their full name"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [{:first-name "Frank" :last-name "Kenstein"}
                                                         {:first-name "Meily" :last-name "User"}])
                            user-list)]
               (-> page (html/select [:.clj--user-item]))
               => (just [(th/text-is? [:.clj--user-item__full-name] "Frank Kenstein")
                         (th/text-is? [:.clj--user-item__full-name] "Meily User")])))

       (fact "when user has trusted role, the checkbox value should be checked"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [{:login "confirmed@email.com" :confirmed? true :role (:trusted config/roles)}])
                            user-list)]
               (-> page
                   (html/select [:.clj--user-item__toggle])
                   first
                   :attrs
                   :checked) => "checked"))

       (fact "when user has untrusted role, the checkbox value should be nil, but input should exist"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [{:login "confirmed@email.com" :confirmed? true :role (:untrusted config/roles)}])
                            user-list)]
               (-> page
                   (html/select [:.clj--user-item__toggle])
                   first
                   :attrs
                   :checked) => nil

               (-> page
                   (html/select [:.clj--user-item])) => (th/element-exists? [:.clj--user-item__toggle])))

       (fact "when there are no users an empty list is rendered"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [])
                            user-list)]
               page => (th/element-exists? [:.clj--user-list__item__empty]))))

(fact
  (let [translator (t/translations-fn t/translation-map)
        request (th/create-request translator)]
    (th/test-translations "user-list" user-list request)))

(facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request) user-list)]
               (-> page (html/select [:.clj--flash-message-text])) => empty?))

       (tabular
         (fact "appropriate flash message is displayed on page when a flash key is included in the request"
               (let [page (-> (th/create-request)
                              (assoc-in [:flash :translation-key] ?flash-key)
                              (assoc-in [:flash :updated-account-email] ?user-login)
                              user-list)]
                 (-> page (html/select [:.clj--flash-message-container])) =not=> empty?
                 (-> page (html/select [:.clj--flash-message-login]) first html/text) => (contains ?user-login)
                 (-> page (html/select [:.clj--flash-message-text]) first :attrs :data-l8n)
                 => ?translation-key))

         ?flash-key                ?user-login                ?translation-key
         :user-trusted             "valid@test.com"           "content:flash/user-trusted"
         :user-untrusted           "valid@test.com"           "content:flash/user-untrusted"))
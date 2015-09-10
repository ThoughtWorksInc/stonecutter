(ns stonecutter.test.view.user-list
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.user-list :refer [user-list]]
            [stonecutter.translation :as t]))

(fact "user-list should return some html"
      (let [page (-> (th/create-request) user-list)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) user-list)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) user-list)]
        page => (th/has-attr? [:.clj--sign-out__link] :href (r/path :sign-out))))

(facts "about the list of users"
       (fact "users are displayed"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [{:login "confirmed@email.com" :confirmed? true :role "default"}
                                                         {:login "unconfirmed@email.com" :confirmed? false :role "default"}])
                            user-list)]
               (-> page
                   (html/select [:.clj--user-item])) => (n-of anything 2)))
       (fact "when there are no users an empty list is rendered"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :users] [])
                            user-list)]
               page => (th/element-exists? [:.clj--user-list__item__empty]))))

(fact
  (let [translator (t/translations-fn t/translation-map)
        request (th/create-request translator)]
    (th/test-translations "user-list" user-list request)))
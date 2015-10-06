(ns stonecutter.test.view.apps-list
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.apps-list :refer [apps-list]]
            [stonecutter.translation :as t]
            [stonecutter.config :as config]))

(fact "user-list should return some html"
      (let [page (-> (th/create-request) apps-list)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) apps-list)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--sign-out__link] :href (r/path :sign-out))))

(fact "user list link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list))))

(fact "apps list link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))))

(future-fact
  (let [translator (t/translations-fn t/translation-map)
        request (th/create-request translator)]
    (th/test-translations "apps-list" apps-list request)))

(fact "no flash messages are displayed by default"
      (let [page (-> (th/create-request) apps-list)]
        (-> page (html/select [:.clj--flash-message-text])) => empty?))

(facts "about the apps list"
       (fact "list of apps is displayed with each's details"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :apps] [{:name          "name-1"
                                                         :client-id     "client-id-1"
                                                         :client-secret "client-secret-1"
                                                         :url           "url-1"}
                                                         {:name          "name-2"
                                                          :client-id     "client-id-2"
                                                          :client-secret "client-secret-2"
                                                          :url           "url-2"}])
                            apps-list)]
               (-> page (html/select [:.clj--admin-app-item]))
               => (just [(th/text-is? [:.clj--admin-app-item__title] "name-1")
                         (th/text-is? [:.clj--admin-app-item__title] "name-2")]))))



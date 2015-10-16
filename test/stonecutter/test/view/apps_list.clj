(ns stonecutter.test.view.apps-list
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.apps-list :refer [apps-list]]
            [stonecutter.translation :as t]
            [stonecutter.config :as config]
            [stonecutter.view.apps-list :as apps-list]))

(fact "user-list should return some html"
      (let [page (-> (th/create-request) apps-list)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) apps-list)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--sign-out__link] :href (r/path :sign-out))))

(fact "profile link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--profile__link] :href (r/path :show-profile))))

(fact "user list link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list))))

(fact "apps list link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))))

(fact "invite link should go to correct endpoint"
      (let [page (-> (th/create-request) apps-list)]
        page => (th/has-attr? [:.clj--invite__link] :href (r/path :show-invite))))

(fact "there are no missing translations"
  (let [translator (t/translations-fn t/translation-map)
        request (th/create-request translator)]
    (th/test-translations "apps-list" apps-list request)))

(fact "no flash messages are displayed by default"
      (let [page (-> (th/create-request) apps-list)]
        (-> page (html/select [:.clj--flash-message-text])) => empty?))

(tabular
  (facts "about the apps list"
         (fact "list of apps is displayed with each's details"
               (let [page (-> (th/create-request)
                              (assoc-in [:context :clients] [{:name          "name-1"
                                                              :client-id     "client-id-1"
                                                              :client-secret "client-secret-1"
                                                              :url           "url-1"}
                                                             {:name          "name-2"
                                                              :client-id     "client-id-2"
                                                              :client-secret "client-secret-2"
                                                              :url           "url-2"}])
                              apps-list)]

                 (-> page (html/select [:.clj--admin-app-item])
                     (nth ?position))
                 => (every-checker
                          (th/text-is? [:.clj--admin-app-item__title] ?item-title)
                          (th/text-is? [:.clj--client-id] ?client-id)
                          (th/text-is? [:.clj--client-secret] ?client-secret)
                          (th/text-is? [:.clj--client-url] ?client-url)
                          (th/has-attr? [:.clj--delete-app__link] :href (r/path :delete-app-confirmation :app-id ?client-id))
                          ))))

  ?position                            ?item-title    ?client-id      ?client-secret      ?client-url
  0                                    "name-1"       "client-id-1"   "client-secret-1"   "url-1"
  1                                    "name-2"       "client-id-2"   "client-secret-2"   "url-2"
  )

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request nil nil {}) apps-list/apps-list)]
        page => (th/has-form-action? (r/path :create-client))
        page => (th/has-form-method? "post")))

(facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request) apps-list)]
               (-> page (html/select [:.func--flash-message-add-container])) => empty?
               (-> page (html/select [:.func--flash-message-delete-container])) => empty?))

       (fact "successful add flash message is displayed on page when a flash key is included in the request"
             (let [page (-> (th/create-request)
                            (assoc-in [:flash :added-app-name] "new-client-name")
                            apps-list)]
               (-> page (html/select [:.clj--flash-message-add-container])) =not=> empty?
               (-> page (html/select [:.clj--flash-message-delete-container])) => empty?
               (-> page (html/select [:.clj--new-app-name]) first html/text) => (contains "new-client-name")))

       (fact "successful delete flash message is displayed on page when a flash key is included in the request"
             (let [page (-> (th/create-request)
                            (assoc-in [:flash :deleted-app-name] "client-name")
                            apps-list)]

               (-> page (html/select [:.clj--flash-message-delete-container])) =not=> empty?
               (-> page (html/select [:.clj--flash-message-add-container])) => empty?
               (-> page (html/select [:.clj--deleted-app-name]) first html/text) => (contains "client-name"))))

(facts "about validation messages"
       (fact "no validation is displayed by default"
             (let [page (-> (th/create-request)
                            apps-list)]
               (fact "no elements have class for styling and unhiding errors"
                     (html/select page [:.form-row--invalid]) => empty?)
               (fact "app name validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
                     (html/select page [:.clj--application-name__validation]) =not=> empty?)
               (fact "app url validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
                     (html/select page [:.clj--application-url__validation]) =not=> empty?)))
       (fact "validation message is displayed when input is empty"
             (let [errors {:app-name :blank}
                   params {:app-name " "}
                   page (-> (th/create-request {} errors params) apps-list)
                   error-translation "content:admin-app-list/app-name-blank-error"]
               (fact "the class for styling errors is added"
                     (html/select page [:.form-row--invalid]) =not=> empty?)
               (fact "application name validation element is present"
                     (html/select page [:.clj--application-name__validation]) =not=> empty?)
               (fact "correct error message is displayed"
                     (html/select page [[:.clj--application-name__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
               (fact "there are no missing translations"
                     (th/test-translations "apps list page" (constantly page))))))
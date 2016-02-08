(ns stonecutter.test.view.change-profile
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.change-profile :refer [change-profile-form]]
            [stonecutter.config :as c]))

(fact "should return some html"
      (let [page (-> (th/create-request)
                     change-profile-form)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) change-profile-form)]
        page => th/work-in-progress-removed))

(fact (th/test-translations "Change profile" change-profile-form))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request) change-profile-form)]
        page => (th/has-form-action? (r/path :change-profile))))

(fact "back link should go to correct endpoint"
      (let [page (-> (th/create-request) change-profile-form)]
        (-> page (html/select [:.clj--change-profile-back__link]) first :attrs :href) => (r/path :show-profile)))

(fact "update profile picture should post to correct endpoint"
      (let [page (-> (th/create-request) change-profile-form)]
        (html/select page [:.clj--update-profile-picture__link]) => (th/has-form-action? (r/path :change-profile))))

(fact "page has script link to javascript file"
      (let [page (-> (th/create-request) change-profile-form)]
        (html/select page [[:script (html/attr= :src "js/main.js")]]) =not=> empty?))

(facts "about displaying navigation bar"
       (fact "profile link should go to the correct end point"
             (let [page (-> (th/create-request) change-profile-form)]
               page => (th/has-attr? [:.clj--profile__link] :href (r/path :show-profile))))

       (fact "apps and users links are not displayed if the user role is not admin"
             (let [page (-> (th/create-request)
                            change-profile-form)]
               (-> page (html/select [:.clj--apps-list__link])) => empty?
               (-> page (html/select [:.clj--users-list__link])) => empty?))

       (fact "apps and users links are displayed and go to the correct endpoint if the user role is admin"
             (let [page (-> (th/create-request)
                            (assoc-in [:session :role] (:admin c/roles))
                            change-profile-form)]
               (-> page (html/select [:.clj--apps-list__link])) =not=> empty?
               (-> page (html/select [:.clj--user-list__link])) =not=> empty?
               page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))
               page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list)))))

(facts "about flash messages"
       (fact "no flash messages are displayed by default"
             (let [page (-> (th/create-request)
                            change-profile-form)]
               (-> page (html/select [:.clj--flash-message-container])) => empty?))

       (tabular
         (fact "appropriate flash message is displayed on page when a flash key is included in the request"
               (let [page (-> (th/create-request) (assoc :flash ?flash-key) change-profile-form)]
                 (-> page (html/select [:.clj--flash-message-container])) =not=> empty?
                 (-> page (html/select [:.clj--flash-message-text]) first :attrs :data-l8n)
                 => ?translation-key))

         ?flash-key                 ?translation-key
         :name-changed             "content:flash/name-changed"
         ))

(facts "about removing elements when there are no errors"
       (let [page (-> (th/create-request) change-profile-form)]
         (fact "validation summary is removed"
               (html/select page [:.clj--validation-summary]) => empty?)
         (fact "no elements have class for styling errors"
               (html/select page [:.form-row--invalid]) => empty?)
         (fact "name validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--change-last-name__validation]) =not=> empty?)
         (fact "name validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--change-first-name__validation]) =not=> empty?)))

(facts "about displaying errors"
       (tabular
         (facts "first name validations"
                (let [errors {:first-name ?error}
                      params {:first-name "some-input-to-be-retained"}
                      page (-> (th/create-request {} errors params) change-profile-form)
                      error-translation (str "content:change-profile-form/" ?translation-key)]
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--first-name :.form-row--invalid]]) =not=> empty?)
                  (fact "name validation element is present"
                        (html/select page [:.clj--change-first-name__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--change-first-name__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is preserved in input field"
                        page => (th/has-attr? [:.clj--change-first-name__input] :value "some-input-to-be-retained"))
                  (fact "there are no missing translations"
                        (th/test-translations "change-profile page" (constantly page)))))
         ?error     ?translation-key
         :blank     "change-first-name-blank-validation-message"
         :too-long  "change-first-name-too-long-validation-message")


       (tabular
         (facts "last name validations"
                (let [errors {:last-name ?error}
                      params {:last-name "some-input-to-be-retained"}
                      page (-> (th/create-request {} errors params) change-profile-form)
                      error-translation (str "content:change-profile-form/" ?translation-key)]
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--last-name :.form-row--invalid]]) =not=> empty?)
                  (fact "last name validation element is present"
                        (html/select page [:.clj--change-last-name__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--change-last-name__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is preserved in input field"
                        page => (th/has-attr? [:.clj--change-last-name__input] :value "some-input-to-be-retained"))
                  (fact "there are no missing translations"
                        (th/test-translations "index page" (constantly page)))))
         ?error     ?translation-key
         :blank     "change-last-name-blank-validation-message"
         :too-long  "change-last-name-too-long-validation-message"))

(facts "fields are prefilled with relevant information"
       (tabular
         (facts "change name fields are pre-filled with current name"
                (let [errors {}
                      params {}
                      first-name "firsty"
                      last-name "lasty"
                      page (-> (th/create-request {} errors params)
                               (assoc-in [:context :user-last-name] last-name)
                               (assoc-in [:context :user-first-name] first-name)
                               change-profile-form)]
                  (fact "field is prefilled with existing value"
                        page => (th/has-attr? [?selector] :value ?value))))
         ?selector                        ?value
         :.clj--change-first-name__input   "firsty"
         :.clj--change-last-name__input  "lasty"))

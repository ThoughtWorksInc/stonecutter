(ns stonecutter.test.view.change-password
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.change-password :refer [change-password-form
                                                      current-password-error-translation-key]]
            [stonecutter.config :as c]))

(fact "should return some html"
      (let [page (-> (th/create-request)
                     change-password-form)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) change-password-form)]
        page => th/work-in-progress-removed))

(fact (th/test-translations "Change password" change-password-form))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request) change-password-form)]
        page => (th/has-form-action? (r/path :change-password))))

(fact "cancel link should go to correct endpoint"
      (let [page (-> (th/create-request) change-password-form)]
        (-> page (html/select [:.clj--change-password-cancel__link]) first :attrs :href) => (r/path :show-profile)))

(fact "page has script link to javascript file"
      (let [page (-> (th/create-request) change-password-form)]
        (html/select page [[:script (html/attr= :src "js/main.js")]]) =not=> empty?))

(facts "about displaying navigation bar"
       (fact "profile link should go to the correct end point"
             (let [page (-> (th/create-request) change-password-form)]
               page => (th/has-attr? [:.clj--profile__link] :href (r/path :show-profile))))

       (fact "apps and users links are not displayed if the user role is not admin"
             (let [page (-> (th/create-request)
                            change-password-form)]
               (-> page (html/select [:.clj--apps-list__link])) => empty?
               (-> page (html/select [:.clj--users-list__link])) => empty?))

       (fact "apps and users links are displayed and go to the correct endpoint if the user role is admin"
             (let [page (-> (th/create-request)
                            (assoc-in [:session :role] (:admin c/roles))
                            change-password-form)]
               (-> page (html/select [:.clj--apps-list__link])) =not=> empty?
               (-> page (html/select [:.clj--user-list__link])) =not=> empty?
               page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))
               page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list)))))

(facts "about removing elements when there are no errors"
       (let [page (-> (th/create-request) change-password-form)]
         (fact "validation summary is removed"
               (html/select page [:.clj--validation-summary]) => empty?)
         (fact "no elements have class for styling errors"
               (html/select page [:.form-row--invalid]) => empty?)
         (fact "current-password validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--current-password__validation]) =not=> empty?)
         (fact "new-password validation element is not removed - it is hidden by not having the <form-row--invalid> in a parent"
               (html/select page [:.clj--new-password__validation]) =not=> empty?)))

(facts "about displaying errors"

       (tabular
         (facts "current-password errors use the same error message"
                (let [errors {:current-password ?error}
                      params {:current-password "invalid-password-not-preserved"}
                      page (-> (th/create-request {} errors params) change-password-form)
                      error-translation current-password-error-translation-key]
                  (fact "validation summary includes the error message"
                        (html/select page [[:.clj--validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--current-password :.form-row--invalid]]) =not=> empty?)
                  (fact "current-password validation element is present"
                        (html/select page [:.clj--current-password__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--current-password__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is not preserved in input field"
                        (-> page (html/select [:.clj--current-password__input]) first :attrs :value) => nil)
                  (fact "there are no missing translations"
                        (th/test-translations "change password page" (constantly page)))))
         ?error
         :blank
         :too-short
         :too-long
         :invalid)

       (tabular
         (facts "new-password error messages and translations"
                (let [errors {:new-password ?error}
                      params {:new-password "invalid-password-not-preserved"}
                      page (-> (th/create-request {} errors params) change-password-form)
                      error-translation ?translation-key]
                  (fact "validation summary includes the error message"
                        (html/select page [[:.clj--validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--new-password :.form-row--invalid]]) =not=> empty?)
                  (fact "new-password validation element is present"
                        (html/select page [:.clj--new-password__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--new-password__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is not preserved in input field"
                        (-> page (html/select [:.clj--new-password__input]) first :attrs :value) => nil)
                  (fact "there are no missing translations"
                        (th/test-translations "change password page" (constantly page)))))

         ?error            ?translation-key
         :blank            "content:change-password-form/new-password-blank-validation-message"
         :too-short        "content:change-password-form/new-password-too-short-validation-message"
         :too-long         "content:change-password-form/new-password-too-long-validation-message"
         :unchanged        "content:change-password-form/new-password-unchanged-validation-message")

       (facts "when there are current password and new password errors"
              (let [errors {:current-password :invalid
                            :new-password :too-short}
                    params {:current-password "not-preserved"
                            :new-password "invalid-password-not-preserved"}
                    page (-> (th/create-request {} errors params) change-password-form)
                    error-translation-1 current-password-error-translation-key
                    error-translation-2 "content:change-password-form/new-password-too-short-validation-message"]
                  (fact "validation summary includes the error messages"
                        (->> (html/select page [:.clj--validation-summary__item])
                                          (map #(get-in % [:attrs :data-l8n])))
                        => [error-translation-1 error-translation-2])
                  (fact "the classes for styling errors is added"
                        (html/select page [[:.clj--current-password :.form-row--invalid]]) =not=> empty?
                        (html/select page [[:.clj--new-password :.form-row--invalid]]) =not=> empty?)
                  (fact "validation elements are present"
                        (html/select page [:.clj--new-password__validation]) =not=> empty?
                        (html/select page [:.clj--new-password__validation]) =not=> empty?)
                  (fact "correct error messages are displayed"
                        (html/select page [[:.clj--current-password__validation (html/attr= :data-l8n error-translation-1)]]) =not=> empty?
                        (html/select page [[:.clj--new-password__validation (html/attr= :data-l8n error-translation-2)]]) =not=> empty?)
                  (fact "invalid values are not preserved in input fields"
                        (-> page (html/select [:.clj--current-password__input]) first :attrs :value) => nil
                        (-> page (html/select [:.clj--new-password__input]) first :attrs :value) => nil)
                  (fact "there are no missing translations"
                        (th/test-translations "change password page" (constantly page))))))

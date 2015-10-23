(ns stonecutter.test.view.change-email
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.change-email :as change-email]
            [stonecutter.config :as c]))

(def)

(fact "should return some html"
      (let [page (-> (th/create-request)
                     change-email/change-email-form)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) change-email/change-email-form)]
        page => th/work-in-progress-removed))

(fact (th/test-translations "Change password" change-email/change-email-form))

(future-fact "form posts to correct endpoint"
      (let [page (-> (th/create-request) change-email/change-email-form)]
        page => (th/has-form-action? (r/path :change-email))))

(fact "cancel link should go to correct endpoint"
      (let [page (-> (th/create-request) change-email/change-email-form)]
        (-> page (html/select [:.clj--change-password-cancel__link]) first :attrs :href) => (r/path :show-profile)))

(future-fact "page has script link to javascript file"
             (let [page (-> (th/create-request) change-email/change-email-form)]
               (html/select page [[:script (html/attr= :src "js/main.js")]]) =not=> empty?))

(facts "about displaying navigation bar"
       (fact "profile link should go to the correct end point"
             (let [page (-> (th/create-request) change-email/change-email-form)]
               page => (th/has-attr? [:.clj--profile__link] :href (r/path :show-profile))))

       (fact "apps and users links are not displayed if the user role is not admin"
             (let [page (-> (th/create-request)
                            change-email/change-email-form)]
               (-> page (html/select [:.clj--apps-list__link])) => empty?
               (-> page (html/select [:.clj--users-list__link])) => empty?))

       (fact "apps and users links are displayed and go to the correct endpoint if the user role is admin"
             (let [page (-> (th/create-request)
                            (assoc-in [:session :role] (:admin c/roles))
                            change-email/change-email-form)]
               (-> page (html/select [:.clj--apps-list__link])) =not=> empty?
               (-> page (html/select [:.clj--user-list__link])) =not=> empty?
               page => (th/has-attr? [:.clj--apps-list__link] :href (r/path :show-apps-list))
               page => (th/has-attr? [:.clj--user-list__link] :href (r/path :show-user-list)))))

(facts "about removing elements when there are no errors"
       (let [page (-> (th/create-request) change-email/change-email-form)]
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
         (facts "new-email error messages and translations"
                (let [errors {:new-email ?error}
                      params {:new-email "invalid-password-not-preserved"}
                      page (-> (th/create-request {} errors params) change-email/change-email-form)
                      error-translation ?translation-key]
                  (fact "validation summary includes the error message"
                        (html/select page [[:.clj--validation-summary__item (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--new-email :.form-row--invalid]]) =not=> empty?)
                  (fact "new-password validation element is present"
                        (html/select page [:.clj--new-email__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--new-email__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "invalid value is not preserved in input field"
                        (-> page (html/select [:.clj--new-email__input]) first :attrs :value) => nil)
                  (fact "there are no missing translations"
                        (th/test-translations "change email page" (constantly page)))))

         ?error       ?translation-key
         :blank       "content:change-email-form/new-email-blank-validation-message"
         :invalid     "content:change-email-form/new-email-invalid-validation-message"
         :duplicate   "content:change-email-form/new-email-duplicate-validation-message"
         :unchanged   "content:change-email-form/new-email-unchanged-validation-message")

       (facts "when there are new email errors"
              (let [errors {:new-email     :invalid}
                    params {:new-email     "invalid-email-not-preserved"}
                    page (-> (th/create-request {} errors params) change-email/change-email-form)
                    error-translation change-email/email-error-translation-key]
                (fact "validation summary includes the error messages"
                      (->> (html/select page [:.clj--validation-summary__item])
                           (map #(get-in % [:attrs :data-l8n])))
                      => [error-translation])
                (fact "validation elements are present"
                      (html/select page [:.clj--new-email__validation]) =not=> empty?)
                (fact "correct error messages are displayed"
                      (html/select page [[:.clj--new-email__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                (fact "invalid values are not preserved in input fields"
                      (-> page (html/select [:.clj--new-email__input]) first :attrs :value) => nil)
                (fact "there are no missing translations"
                      (th/test-translations "change email page" (constantly page))))))

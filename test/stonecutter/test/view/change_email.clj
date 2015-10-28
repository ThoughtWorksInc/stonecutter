(ns stonecutter.test.view.change-email
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.routes :as r]
            [stonecutter.view.change-email :as change-email]
            [stonecutter.config :as c]))

(fact "should return some html"
      (let [page (-> (th/create-request)
                     change-email/change-email-form)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) change-email/change-email-form)]
        page => th/work-in-progress-removed))

(fact "page should be translated"
      (th/test-translations "Change email" change-email/change-email-form))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request) change-email/change-email-form)]
        page => (th/has-form-action? (r/path :change-email))))

(fact "cancel link should go to correct endpoint"
      (let [page (-> (th/create-request) change-email/change-email-form)]
        (-> page (html/select [:.clj--change-email-cancel__link]) first :attrs :href) => (r/path :show-profile)))

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
         (fact "there are no errors by default"
               (html/select page [:.form-row--invalid]) => empty?)))

(facts "about displaying errors"

       (tabular
         (facts "new-email error messages and translations"
                (let [errors {:new-email ?error}
                      params {:new-email "invalid-email@somewhere.com"}
                      page (-> (th/create-request {} errors params) change-email/change-email-form)
                      error-translation ?translation-key]
                  (fact "the class for styling errors is added"
                        (html/select page [[:.clj--email-address :.form-row--invalid]]) =not=> empty?)
                  (fact "new-email validation element is present"
                        (html/select page [:.clj--new-email__validation]) =not=> empty?)
                  (fact "correct error message is displayed"
                        (html/select page [[:.clj--new-email__validation (html/attr= :data-l8n error-translation)]]) =not=> empty?)
                  (fact "there are no missing translations"
                        (th/test-translations "change email page" (constantly page)))))

         ?error       ?translation-key
         :blank       "content:change-email-form/new-email-blank-validation-message"
         :invalid     "content:change-email-form/new-email-invalid-validation-message"
         :duplicate   "content:change-email-form/new-email-duplicate-validation-message"
         :unchanged   "content:change-email-form/new-email-unchanged-validation-message"
         ))

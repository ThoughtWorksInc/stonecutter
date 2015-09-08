(ns stonecutter.test.view.delete-account
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.routes :as r]
            [stonecutter.view.delete-account :refer [delete-account-confirmation
                                                     profile-deleted
                                                     email-confirmation-delete-account]]
            [stonecutter.helper :as helper]))

(facts "about delete-account-confirmation page"
       (fact "should return some html"
             (let [page (-> (th/create-request)
                            delete-account-confirmation)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request) delete-account-confirmation)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn t/translation-map)
                   page (-> (th/create-request) delete-account-confirmation (helper/enlive-response {:translator translator}) :body)]
               page => th/no-untranslated-strings))

       (fact "form posts to correct endpoint"
             (let [page (-> (th/create-request) delete-account-confirmation)]
               (-> page (html/select [:form]) first :attrs :action) => (r/path :delete-account)))

       (fact "cancel link should go to correct endpoint"
             (let [page (-> (th/create-request) delete-account-confirmation)]
               (-> page (html/select [:.clj--delete-account-cancel__link]) first :attrs :href) => (r/path :show-profile))))

(facts "about email-confirmation-delete-account page"
       (fact "should return some html"
             (let [page (-> (th/create-request)
                            email-confirmation-delete-account)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request) email-confirmation-delete-account)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn t/translation-map)
                   page (-> (th/create-request) email-confirmation-delete-account (helper/enlive-response {:translator translator}) :body)]
               page => th/no-untranslated-strings))

       (fact "form posts to correct endpoint"
             (let [page (-> (th/create-request)
                            (assoc :params {:confirmation-id "some-confirmation-id"})
                            email-confirmation-delete-account)]
               (-> page (html/select [:form]) first :attrs :action) => (r/path :show-confirmation-delete
                                                                               :confirmation-id "some-confirmation-id")))

       (fact "cancel link should go to correct endpoint"
             (let [page (-> (th/create-request) email-confirmation-delete-account)]
               (-> page (html/select [:.clj--delete-account-cancel__link]) first :attrs :href) => (r/path :index))))

(facts "about show-profile-deleted page"
       (fact "should return some html"
             (let [page (-> (th/create-request)
                            profile-deleted)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request) profile-deleted)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn t/translation-map)
                   page (-> (th/create-request) profile-deleted (helper/enlive-response {:translator translator}) :body)]
               page => th/no-untranslated-strings)))

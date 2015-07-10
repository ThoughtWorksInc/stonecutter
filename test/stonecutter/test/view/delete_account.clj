(ns stonecutter.test.view.delete-account
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.view.delete-account :refer [delete-account-confirmation
                                                     profile-deleted]]
            [stonecutter.translation :as t]))

(facts "about delete-account-confirmation page"
       (fact "should return some html"
             (let [page (-> (th/create-request {} nil {})
                            delete-account-confirmation
                            html/html-snippet)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request {} nil {}) delete-account-confirmation html/html-snippet)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn t/translation-map)
                   page (-> (th/create-request translator nil {}) delete-account-confirmation)]
               page => th/no-untranslated-strings)))

(facts "about show-profile-deleted page"
       (fact "should return some html"
             (let [page (-> (th/create-request {} nil {})
                            profile-deleted
                            html/html-snippet)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request {} nil {}) profile-deleted html/html-snippet)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn t/translation-map)
                   page (-> (th/create-request translator nil {}) profile-deleted)]
               page => th/no-untranslated-strings)))

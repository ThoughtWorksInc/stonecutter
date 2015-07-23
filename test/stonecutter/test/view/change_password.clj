(ns stonecutter.test.view.change-password
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.routes :as r]
            [stonecutter.helper :as helper]
            [stonecutter.view.change-password :refer [change-password-form]]))

(facts "about change-password page"
       (fact "should return some html"
             (let [page (-> (th/create-request)
                            change-password-form)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request) change-password-form)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn t/translation-map)
                   page (-> (th/create-request) change-password-form (helper/enlive-response {:translator translator}) :body)]
               page => th/no-untranslated-strings))

       (fact "form posts to correct endpoint"
             (let [page (-> (th/create-request) change-password-form)]
               (-> page (html/select [:form]) first :attrs :action) => (r/path :change-password)))

       (fact "cancel link should go to correct endpoint"
             (let [page (-> (th/create-request) change-password-form)]
               (-> page (html/select [:.clj--change-password-cancel__link]) first :attrs :href) => (r/path :show-profile))))

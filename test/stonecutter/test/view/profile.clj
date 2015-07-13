(ns stonecutter.test.view.profile
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.profile :refer [profile]]))

(fact "profile should return some html"
      (let [page (-> (th/create-request)
                     profile
                     html/html-snippet)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) profile html/html-snippet)]
        page => th/work-in-progress-removed))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (th/create-request) profile html/html-snippet)]
        (-> page (html/select [:.func--sign-out__link]) first :attrs :href) => (r/path :sign-out)))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator) profile)]
        page => th/no-untranslated-strings))

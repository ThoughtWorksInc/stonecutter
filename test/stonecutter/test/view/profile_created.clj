(ns stonecutter.test.view.profile-created
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.profile-created :refer [profile-created]]))

(fact "profile-created should return some html"
      (let [page (-> (th/create-request {} nil {})
                     profile-created
                     html/html-snippet)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request {} nil {}) profile-created html/html-snippet)]
        page => th/work-in-progress-removed))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator nil {}) profile-created)]
        page => th/no-untranslated-strings))

(fact "when registering on stonecutter, next button should default to profile page"
      (let [page (-> (th/create-request {} nil {})
                     profile-created
                     html/html-snippet)]
        (-> page (html/select [:.func--profile-created-next__button]) first :attrs :href) => (r/path :show-profile)))

(fact "when coming from authorisation flow, next button should go to authorisation form"
      (let [page (-> (th/create-request {} nil {:from-app true})
                     profile-created
                     html/html-snippet)]
        (-> page (html/select [:.func--profile-created-next__button]) first :attrs :href) => (r/path :show-authorise-form)))


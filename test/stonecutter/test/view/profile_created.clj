(ns stonecutter.test.view.profile-created
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.profile-created :refer [profile-created]]
            [stonecutter.helper :as helper]))

(fact "profile-created should return some html"
      (let [page (-> (th/create-request)
                     profile-created)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) profile-created)]
        page => th/work-in-progress-removed))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request) profile-created (helper/enlive-response {:translator translator}) :body)]
        page => th/no-untranslated-strings))

(facts "when registering on stonecutter"
       (let [page (-> (th/create-request)
                      profile-created)]

         (fact "next button should default to profile page"
               (-> page (html/select [:.func--profile-created-next__button]) first :attrs :href) => (r/path :show-profile))

         (fact "next button should use default button text"
               (-> page (html/select [:.func--profile-created-next__button]) first :attrs :data-l8n) => "content:profile-created/action-button-default")))

(facts "when coming from authorisation flow"
       (let [page (-> (th/create-request {} nil {:from-app true} {:return-to "land of milk and honey"})
                      profile-created)]

         (fact "when coming from authorisation flow, next button should go to authorisation form"
               (-> page (html/select [:.func--profile-created-next__button]) first :attrs :href) => "land of milk and honey")

         (fact "when coming from authorisation flow, next button should use from-app text"
               (-> page (html/select [:.func--profile-created-next__button]) first :attrs :data-l8n) => "content:profile-created/action-button-from-app")))

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

(facts "about displaying authorised clients"
       (fact "names of authorised clients are displayed"
             (let [page (-> (th/create-request)
                            (assoc-in [:context :authorised-clients] [{:name "Bloc Party"} {:name "Tabletennis Party"}])
                            profile
                            html/html-snippet)]
               (-> page 
                   (html/select [:.func--app__list])
                   first
                   html/text) => (contains #"Bloc Party[\s\S]+Tabletennis Party"))) 

       (fact "empty application-list item is used when there are no authorised clients"
             (let [page (-> (th/create-request)
                            profile
                            html/html-snippet)]
                (html/select page [:.clj--authorised-app__list-item--empty]) =not=> empty?
                (html/select page [:.clj--authorised-app__list-item]) => empty?))) 

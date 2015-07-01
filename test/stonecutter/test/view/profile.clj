(ns stonecutter.test.view.profile 
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.handler :refer [translations-fn translation-map]]
            [stonecutter.test.view.test-helpers :refer [create-request]]
            [stonecutter.view.profile :refer [profile]]))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(fact "profile should return some html"
      (let [page (-> (create-request {} nil {})
                     profile
                     html/html-snippet)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (create-request {} nil {}) profile html/html-snippet)]
        (-> page (html/select [:.clj-wip])) => empty?))

(fact "sign out link should go to correct endpoint"
      (let [page (-> (create-request {} nil {}) profile html/html-snippet)]
        (-> page (html/select [:.func--sign-out__link]) first :attrs :href) => (r/path :sign-out)))

(fact "there are no missing translations"
      (let [translator (translations-fn translation-map)
            page (-> (create-request translator nil {}) profile)]
        page => no-untranslated-strings))

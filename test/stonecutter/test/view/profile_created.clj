(ns stonecutter.test.view.profile-created
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.handler :refer [translations-fn translation-map]]
            [stonecutter.test.view.test-helpers :refer [create-request]]
            [stonecutter.view.profile-created :refer [profile-created]]))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(fact "profile-created should return some html"
      (let [page (-> (create-request {} nil {})
                     profile-created
                     html/html-snippet)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (create-request {} nil {}) profile-created html/html-snippet)]
        (-> page (html/select [:.clj-wip])) => empty?))


(fact "there are no missing translations"
      (let [translator (translations-fn translation-map)
            page (-> (create-request translator nil {}) profile-created)]
        page => no-untranslated-strings))

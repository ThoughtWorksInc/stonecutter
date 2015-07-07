(ns stonecutter.test.view.authorise
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :refer [create-request]]
            [stonecutter.view.authorise :refer [authorise-form]]
            [stonecutter.translation :as t]))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(fact "authorise should return some html"
      (let [page (-> (create-request {} nil {})
                     authorise-form
                     html/html-snippet)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (create-request {} nil {}) authorise-form html/html-snippet)]
        (-> page (html/select [:.clj-wip])) => empty?))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (create-request translator nil {}) authorise-form)]
        page => no-untranslated-strings))

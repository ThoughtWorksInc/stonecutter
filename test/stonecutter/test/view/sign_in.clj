(ns stonecutter.test.view.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.handler :refer [translations-fn translation-map]]
            [stonecutter.view.sign-in :refer [sign-in-form]]))

(defn create-context [translator err params]
  {:translator translator
   :errors err
   :params params})

(def no-untranslated-strings 
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(fact "sign-in-form should return some html"
      (let [page (-> (create-context {} nil {}) sign-in-form html/html-snippet)]
        (html/select page [:form]) =not=> empty?))

(fact "form should have correct action"
      (let [page (-> (create-context {} nil {}) sign-in-form html/html-snippet)]
        (-> page (html/select [:form]) first :attrs :action) => "/sign-in"))

(fact "there are no missing translations"
      (stonecutter.logging/init-logger!)
      (let [translator (translations-fn translation-map)
            page (-> (create-context translator nil {}) sign-in-form)]
        page => no-untranslated-strings))

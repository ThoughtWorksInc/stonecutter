(ns stonecutter.test.view.authorise-failure
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.view.authorise-failure :refer [show-authorise-failure]]
            [stonecutter.translation :as t]))

(fact "authorise should return some html"
      (let [page (-> (th/create-request {} nil {})
                     show-authorise-failure
                     html/html-snippet)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request {} nil {}) show-authorise-failure html/html-snippet)]
        page => th/work-in-progress-removed))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator nil {}) show-authorise-failure)]
        page => th/no-untranslated-strings))

(fact "redirect-uri from session is set as return to client link"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator nil {})
                     (assoc-in [:session :redirect-uri] "redirect-uri")
                     show-authorise-failure
                     html/html-snippet)]
        (-> page (html/select [:.func--redirect-to-client-home__link]) first :attrs :href) => "redirect-uri"))

(fact "client name is set on the page"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator nil {})
                     (assoc-in [:session :client-name] "Super Client App")
                     show-authorise-failure
                     html/html-snippet)]
        (-> page (html/select [:.clj--app-name]) first :content) => (contains "Super Client App")))

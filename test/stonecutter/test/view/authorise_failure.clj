(ns stonecutter.test.view.authorise-failure
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.view.authorise-failure :refer [show-authorise-failure]]
            [stonecutter.translation :as t]
            [stonecutter.helper :as helper]))

(fact "authorise should return some html"
      (let [page (-> (th/create-request)
                     show-authorise-failure)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) show-authorise-failure)]
        page => th/work-in-progress-removed))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request) show-authorise-failure (helper/enlive-response {:translator translator}) :body)]
        page => th/no-untranslated-strings))

(fact "redirect-uri from session is set as return to client link"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator)
                     (assoc-in [:params :callback-uri-with-error] "redirect-uri?error=access_denied")
                     show-authorise-failure)]
        (-> page (html/select [:.func--redirect-to-client-home__link]) first :attrs :href) => "redirect-uri?error=access_denied"))

(fact "client name is set on the page"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request translator)
                     (assoc-in [:session :client-name] "Super Client App")
                     show-authorise-failure)]
        (-> page (html/select [:.clj--app-name]) first :content) => (contains "Super Client App")))

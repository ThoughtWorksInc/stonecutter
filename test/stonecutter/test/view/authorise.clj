(ns stonecutter.test.view.authorise
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.view.authorise :refer [authorise-form]]
            [stonecutter.helper :as helper]))

(fact "authorise should return some html"
      (let [page (-> (th/create-request) authorise-form)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) authorise-form)]
        page => th/work-in-progress-removed))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request) authorise-form (helper/enlive-response {:translator translator}) :body)]
        page => th/no-untranslated-strings))

(fact "authorise form posts to correct endpoint"
      (let [page (-> (th/create-request) authorise-form)]
        (-> page (html/select [:.func--authorise__form]) first :attrs :action) => (r/path :authorise-client)))

(fact "cancel link should go to correct endpoint"
      (let [params {:client_id "CLIENT_ID" :redirect_uri "http://where.to.now"}
            page (-> (th/create-request {} nil params) authorise-form)]
        (-> page (html/select [:.func--authorise-cancel__link]) first :attrs :href)
        => (str (r/path :show-authorise-failure)
                "?client_id=CLIENT_ID"
                "&redirect_uri=http://where.to.now")))

(fact "client name is injected"
      (let [client-name "CLIENT_NAME"
            client-name-elements (-> (th/create-request)
                                     (assoc-in [:context :client :name] client-name)
                                     authorise-form
                                     (html/select [:.clj--client-name]))
            client-name-is-correct-fn (fn [element] (= (html/text element) client-name))]
        client-name-elements =not=> empty?
        client-name-elements => (has every? client-name-is-correct-fn)))

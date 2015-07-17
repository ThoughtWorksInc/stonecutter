(ns stonecutter.test.view.unshare-profile-card
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.routes :as r]
            [stonecutter.view.unshare-profile-card :refer [unshare-profile-card]]
            [stonecutter.helper :as helper]))

(fact "should return some html"
      (let [page (-> (th/create-request)
                     unshare-profile-card)]
        (html/select page [:body]) =not=> empty?))

(fact "work in progress should be removed from page"
      (let [page (-> (th/create-request) unshare-profile-card)]
        page => th/work-in-progress-removed))

(fact "there are no missing translations"
      (let [translator (t/translations-fn t/translation-map)
            page (-> (th/create-request) unshare-profile-card (helper/enlive-response {:translator translator}) :body)]
        page => th/no-untranslated-strings))

(fact "form posts to correct endpoint"
      (let [page (-> (th/create-request) unshare-profile-card)]
        (-> page (html/select [:form]) first :attrs :action) => (r/path :unshare-profile-card)))

(fact "client_id is included in the form as a hidden parameter"
      (let [client-id-element (-> (th/create-request)
                                  (assoc-in [:context :client] {:client-id "SOME_CLIENT_ID"})
                                  unshare-profile-card
                                  (html/select [:.clj--client-id__input])
                                  first)]
        (-> client-id-element :attrs :value) => "SOME_CLIENT_ID"
        (-> client-id-element :attrs :type) => "hidden"))

(fact "app name is injected"
      (let [client-name "CLIENT_NAME"
            app-name-elements (-> (th/create-request)
                                  (assoc-in [:context :client] {:name client-name})
                                  unshare-profile-card
                                  (html/select [:.clj--app-name]))
            app-name-is-correct-fn (fn [element] (= (html/text element) client-name))]
        app-name-elements =not=> empty?
        app-name-elements => (has every? app-name-is-correct-fn)))

(fact "cancel link should go to correct endpoint"
      (let [page (-> (th/create-request) unshare-profile-card)]
        (-> page (html/select [:.clj--unshare-profile-card-cancel__link]) first :attrs :href) => (r/path :show-profile)))

(ns stonecutter.test.view.delete-app
  (:require [midje.sweet :refer :all]
            [stonecutter.routes :as r]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.translation :as t]
            [stonecutter.helper :as helper]
            [stonecutter.view.delete-app :refer [delete-app-confirmation]]))

(defn assoc-app-id [request id]
  (assoc-in request [:params :app-id] id))


(facts "about delete-app-confirmation page"
       (fact "should return some html"
             (let [page (-> (th/create-request)
                            (assoc-app-id "blah")
                            delete-app-confirmation)]
               (html/select page [:body]) =not=> empty?))

       (fact "work in progress should be removed from page"
             (let [page (-> (th/create-request)
                            (assoc-app-id "blah")
                            delete-app-confirmation)]
               page => th/work-in-progress-removed))

       (fact "there are no missing translations"
             (let [translator (t/translations-fn
                                t/translation-map)
                   page (-> (th/create-request)
                            (assoc-app-id "blah")
                            delete-app-confirmation
                            (helper/enlive-response {:translator translator}) :body)]
               page => th/no-untranslated-strings))

       (fact "form posts to correct endpoint"
             (let [page (-> (th/create-request)
                            (assoc-app-id "blah")
                            delete-app-confirmation)]
               (-> page (html/select [:form]) first :attrs :action) => (r/path :delete-app :app-id "blah")))

       (fact "cancel link should go to correct endpoint"
             (let [page (-> (th/create-request)
                            (assoc-app-id "blah")
                            delete-app-confirmation)]
               (-> page (html/select [:.clj--delete-app-cancel__link]) first :attrs :href) => (r/path :show-apps-list))))

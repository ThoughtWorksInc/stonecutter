(ns stonecutter.test.view.error
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.helper :as helper]
            [stonecutter.translation :as t]
            [stonecutter.routes :as r]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.view.error :as e]))
(let [request {:params {} :error-m {} :session {:locale :fi}}]
  (fact "modify error translation keys updates the data-l8n tags of the correct elements"
        (let [modified-error-enlive-map (e/modify-error-translation-keys (e/internal-server-error request) "oops-error")]
          modified-error-enlive-map => (th/has-attr? [:body]
                                                     :class "func--oops-error-page")
          modified-error-enlive-map => (th/has-attr? [:title]
                                                     :data-l8n "content:oops-error/title")
          modified-error-enlive-map => (th/has-attr? [:.clj--error-page-header]
                                                     :data-l8n "content:oops-error/page-header")
          modified-error-enlive-map => (th/has-attr? [:.clj--error-page-intro]
                                                     :data-l8n "content:oops-error/page-intro")))

  (tabular
    (facts "there are no missing translations"
           (let [translator (t/translations-fn t/translation-map)
                 page (-> (?error-page-fn request) (helper/enlive-response {:translator translator}) :body)]
             page => th/no-untranslated-strings))
    ?error-page-fn
    e/account-nonexistent
    e/csrf-error
    e/forbidden-error
    e/internal-server-error
    e/not-found-error)

  (fact "home page link is set correctly"
        (-> (e/not-found-error request) (html/select [:.clj--error-return-home__link]) first :attrs :href) => (r/path :index)))

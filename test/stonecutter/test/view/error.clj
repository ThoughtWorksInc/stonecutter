(ns stonecutter.test.view.error
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.test.view.test-helpers :as th]
            [stonecutter.view.error :as e]))

(fact "modify error translation keys updates the data-l8n tags of the correct elements"
      (let [modified-error-enlive-map (e/modify-error-translation-keys (e/internal-server-error) "oops-error")]
        modified-error-enlive-map => (th/has-attr? [:body]
                                                   :class "func--oops-error-page")
        modified-error-enlive-map => (th/has-attr? [:title]
                                                   :data-l8n "content:oops-error/title")
        modified-error-enlive-map => (th/has-attr? [:.clj--error-page-header]
                                                   :data-l8n "content:oops-error/page-header")
        modified-error-enlive-map => (th/has-attr? [:.clj--error-page-intro]
                                                   :data-l8n "content:oops-error/page-intro")
        modified-error-enlive-map => (th/has-attr? [:.clj--error-page-content]
                                                   :data-l8n "html:oops-error/page-content")))

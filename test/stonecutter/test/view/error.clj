(ns stonecutter.test.view.error
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.error :as e]))

(fact "modify error translation keys updates the data-l8n tags of the correct elements"
      (let [modified-error-enlive-map (e/modify-error-translation-keys (e/internal-server-error) "oops-error")]
        (-> (html/select modified-error-enlive-map [:body])
            first :attrs :class) => "func--oops-error-page"
        (-> (html/select modified-error-enlive-map [:title])
            first :attrs :data-l8n) => "content:oops-error/title"
        (-> (html/select modified-error-enlive-map [:.clj--error-page-header])
            first :attrs :data-l8n) => "content:oops-error/page-header"
        (-> (html/select modified-error-enlive-map [:.clj--error-page-intro])
            first :attrs :data-l8n) => "content:oops-error/page-intro"
        (-> (html/select modified-error-enlive-map [:.clj--error-page-content])
            first :attrs :data-l8n) => "html:oops-error/page-content"))

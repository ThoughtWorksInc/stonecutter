(ns stonecutter.test.view.error
  (:require [midje.sweet :refer :all]
            [stonecutter.view.error :as e]
            [net.cgrand.enlive-html :as html]))

(fact "csrf error has correct page-intro translation key"
      (let [page (html/html-snippet (e/csrf-error {:translator identity}))]
        (-> (html/select page [:.clj--error-info]) first :attrs :data-l8n) => "content:error-403/page-intro"))

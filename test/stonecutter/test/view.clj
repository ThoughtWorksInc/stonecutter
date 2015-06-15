(ns stonecutter.test.view
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view :refer [registration-form]]
            ))

(fact "registration-form should return some html"
      (let [page (-> (registration-form) html/html-snippet)]
        (-> page (html/select [:html])) =not=> empty?
        (-> page (html/select [:form])) =not=> empty?
        ))

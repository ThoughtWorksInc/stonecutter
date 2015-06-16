(ns stonecutter.test.view
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view :refer [registration-form add-anti-forgery]]
            ))

(fact "registration-form should return some html"
      (let [page (-> (registration-form) 
                     html/html-snippet)]
        (-> page 
            (html/select [:form])) =not=> empty?))

(fact "can inject anti-forgery token"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (-> page
            add-anti-forgery
            (html/select [:form (html/attr= :name "__anti-forgery-token")])
            ) =not=> empty?))

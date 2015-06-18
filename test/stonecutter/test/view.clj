(ns stonecutter.test.view
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view :refer [registration-form add-anti-forgery]]
            ))

(def default-translator {})

(fact "registration-form should return some html"
      (let [page (-> default-translator 
                     (registration-form nil) 
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


(fact "there is no error message class if no error is passed"
      (let [page (-> default-translator
                     (registration-form nil) 
                     html/html-snippet)]
        (-> page 
            (html/select [:.form-row--validation-error])) => empty?))

(fact "there is an error message class if an error is passed"
      (let [error-message "Email address is invalid"
            page (-> default-translator
                     (registration-form error-message) 
                     html/html-snippet)]
        (-> page 
            (html/select [:.form-row--validation-error])) =not=> empty?))

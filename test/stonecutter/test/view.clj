(ns stonecutter.test.view
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view :refer [registration-form add-anti-forgery]]
            ))

(def default-translator {})

(defn create-context [err params]
  {:translator default-translator
   :errors err
   :params params
   } 
  )

(fact "registration-form should return some html"
      (let [page (-> (create-context nil {}) 
                     registration-form 
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
      (let [page (-> (create-context nil {})
                     registration-form 
                     html/html-snippet)]
        (-> page 
            (html/select [:.form-row--validation-error])) => empty?))

(fact "there is an error message class if an error is passed"
      (let [error-message "Email address is invalid"
            params {:email "invalid"}
            page (-> (create-context error-message params)
                     registration-form 
                     html/html-snippet)]
        (-> page 
            (html/select [:.form-row--validation-error])) =not=> empty?
        (fact "invalid value is preserved in input field"
              (-> page (html/select [:.registration-email-input]) first :attrs :value) => "invalid")))

(ns stonecutter.test.view
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view :refer [registration-form add-anti-forgery]]))

(defn create-context [err params]
  {:translator {}
   :errors err
   :params params})

(def long-email-address
  (apply str (repeat 255 "x")))

(fact "registration-form should return some html"
      (let [page (-> (create-context nil {}) 
                     registration-form 
                     html/html-snippet)]
        (-> page 
            (html/select [:form])) =not=> empty?))

(fact "form should have correct action"
      (let [page (-> (create-context nil {}) registration-form html/html-snippet)]
        (-> page (html/select [:form]) first :attrs :action) => "/register"))

(fact "can inject anti-forgery token"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (-> page
            add-anti-forgery
            (html/select [:form (html/attr= :name "__anti-forgery-token")])) =not=> empty?))


(fact "there is no error message class if no error is passed"
      (let [page (-> (create-context nil {})
                     registration-form 
                     html/html-snippet)]
        (-> page 
            (html/select [:.form-row--validation-error])) => empty?))

(defn p [v] (prn v) v)

(facts "there is an error message class if an error is passed"
       (fact "email invalid"
         (let [errors {:email :invalid} 
               params {:email "invalid"}
               page (-> (create-context errors params)
                        registration-form 
                        html/html-snippet)]
           (-> page 
               (html/select [[:.clj--registration-email :.form-row--validation-error]])) =not=> empty?
           (fact "invalid value is preserved in input field"
                 (-> page (html/select [:.registration-email-input]) first :attrs :value) => "invalid")))

       (fact "email is a duplicate"
             (let [errors {:email :duplicate}
                   params {:email "valid@email.com"}
                   page (-> (create-context errors params) registration-form html/html-snippet)]
               (-> page (html/select [:.clj--registration-email [:.form-row__validation (html/attr= :data-l8n "content:registration-form/email-address-duplicate-validation-message")]])) =not=> empty?))

       (fact "email is too long"
             (let [errors {:email :too-long}
                   params {:email long-email-address}
                   page (-> (create-context errors params) registration-form html/html-snippet)]
               (-> page (html/select [:.clj--registration-email [:.form-row__validation (html/attr= :data-l8n "content:registration-form/email-address-too-long-validation-message")]])) =not=> empty?))

       (fact "password invalid"
             (let [errors {:password :invalid} 
                   params {:password ""}
                   page (-> (create-context errors params)
                            registration-form 
                            html/html-snippet)]
               (-> page 
                   (html/select [[:.clj--registration-password :.form-row--validation-error]])) =not=> empty?))

       (fact "confirm password invalid"
             (let [errors {:confirm-password :invalid} 
                   params {:password "password" :confirm-password "invalid-password"}
                   page (-> (create-context errors params)
                            registration-form 
                            html/html-snippet)]
               (-> page (html/select [[:.validation-summary__item (html/attr= :data-l8n "content:registration-form/confirm-password-invalid-validation-message")]])) =not=> empty?)))

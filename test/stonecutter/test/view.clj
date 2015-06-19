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

(defn p [v] (prn v) v)

(facts "there is an error message class if an error is passed"
       (fact "email invalid"
         (let [errors {:email :invalid} 
               params {:email "invalid"}
               page (-> (create-context errors params)
                        registration-form 
                        html/html-snippet)]
           (-> page 
               p
               (html/select [[:.registration-email :.form-row--validation-error]])) =not=> empty?
           (fact "invalid value is preserved in input field"
                 (-> page (html/select [:.registration-email-input]) first :attrs :value) => "invalid")))

       (fact "password invalid"
             (let [errors {:password :invalid} 
                   params {:password ""}
                   page (-> (create-context errors params)
                            registration-form 
                            html/html-snippet)]
               (-> page 
                   (html/select [[:.registration-password :.form-row--validation-error]])) =not=> empty?))

       (fact "confirm password invalid"
             (let [errors {:confirm-password :invalid} 
                   params {:password "password" :confirm-password "invalid-password"}
                   page (-> (create-context errors params)
                            registration-form 
                            html/html-snippet)]
               (-> page 
                   (html/select [[:.registration-confirm-password :.form-row--validation-error]])) =not=> empty?))

       ) 

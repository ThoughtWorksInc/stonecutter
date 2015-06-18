(ns stonecutter.view
  (:require [hiccup.core :as hiccup]
            [hiccup.form :as form]
            [traduki.core :as t]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [net.cgrand.jsoup :as jsoup]
            ))

#_(defn registration-form [] 
  (hiccup/html  
    (form/form-to  [:post "/register"]
                  (anti-forgery-field)
                  (form/text-field "username")
                  (form/password-field "password")
                  (form/submit-button "Submit")))) 

(defn anti-forgery-snippet []
  (html/html-snippet (anti-forgery-field)))

(defn add-anti-forgery [enlive-m]
  (html/at enlive-m 
           [:form] (html/prepend (anti-forgery-snippet))))

(defn add-registration-errors [err enlive-m]
  (if err
    (html/at enlive-m 
             [:.registration-email] (html/add-class "form-row--validation-error")) 
    enlive-m))

(defn add-params [params enlive-m]
      (html/at enlive-m
               [:.registration-email-input] (html/set-attr :value (:email params))))

(defn p [v] (prn v) v)

(defn registration-form [context]
  (let [err (:errors context)
        translator (:translator context)
        params (:params context)
        ]
    (->> (html/html-resource "public/register.html")
         add-anti-forgery
         (add-registration-errors err) 
         (add-params params)
         (t/translate translator)
         html/emit*
         (apply str)
         )))


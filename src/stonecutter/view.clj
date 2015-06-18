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
           [:form] (html/prepend (anti-forgery-snippet))
           )
  )

(defn print [something]
  (prn something)
  something
  )

(defn registration-form [translator]
  (->> (html/html-resource "public/register.html")
       add-anti-forgery
       (t/translate translator)
       html/emit*
       (apply str)
       ))


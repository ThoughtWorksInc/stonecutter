(ns stonecutter.view
  (:require [traduki.core :as t]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]))

(defn anti-forgery-snippet []
  (html/html-snippet (anti-forgery-field)))

(defn add-anti-forgery [enlive-m]
  (html/at enlive-m
           [:form] (html/prepend (anti-forgery-snippet))))

(defn add-error-class [enlive-m errors err-key field-row-selector]
  (if (contains? errors err-key)
    (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error"))
    enlive-m))

(def error-translations
  {:email {:invalid "content:registration-form/email-address-invalid-validation-message"
           :duplicate "content:registration-form/email-address-duplicate-validation-message"}})

(defn add-email-error [enlive-m err]
  (let [error-translation (get-in error-translations [:email (:email err)])]
    (-> enlive-m
        (add-error-class err :email [:.clj--registration-email])
        (html/at [:.clj--registration-email :.form-row__validation] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))
        )))

(defn add-password-error [enlive-m err]
  (add-error-class enlive-m err :password [:.clj--registration-password]))

(defn add-confirm-password-error [enlive-m err]
  (add-error-class enlive-m err :confirm-password [:.clj--registration-confirm-password]))

(defn add-registration-errors [err enlive-m]
  (-> enlive-m
      (add-email-error err)
      (add-password-error err)
      (add-confirm-password-error err)))

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
         (apply str))))


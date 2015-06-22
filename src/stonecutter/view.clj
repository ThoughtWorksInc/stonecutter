(ns stonecutter.view
  (:require [traduki.core :as t]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(defn anti-forgery-snippet []
  (html/html-snippet (anti-forgery-field)))

(defn add-anti-forgery [enlive-m]
  (html/at enlive-m
           [:form] (html/prepend (anti-forgery-snippet))))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(def error-translations
  {:email {:invalid "content:registration-form/email-address-invalid-validation-message"
           :duplicate "content:registration-form/email-address-duplicate-validation-message"
           :too-long "content:registration-form/email-address-too-long-validation-message"}
   :confirm-password {:invalid "content:registration-form/confirm-password-invalid-validation-message"}})

(defn add-email-error [enlive-m err]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--registration-email])
          (html/at [:.clj--registration-email :.form-row__validation] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    enlive-m))

(defn add-password-error [enlive-m err]
  (if (contains? err :password)
    (add-error-class enlive-m [:.clj--registration-password])
    enlive-m))

(defn add-confirm-password-error [enlive-m err]
  (if (contains? err :confirm-password)
    (let [error-translation (get-in error-translations [:confirm-password (:confirm-password err)])]
      (-> enlive-m
          (html/at [:.validation-summary] (html/remove-attr :hidden))
          (html/at [:li.validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    enlive-m))

(defn add-registration-errors [err enlive-m]
  (-> enlive-m
      (add-email-error err)
      (add-password-error err)
      (add-confirm-password-error err)))

(defn add-params [params enlive-m]
  (html/at enlive-m
           [:.registration-email-input] (html/set-attr :value (:email params))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :register-user))))

(defn p [v] (prn v) v)

(defn registration-form [context]
  (let [err (:errors context)
        translator (:translator context)
        params (:params context)]
    (->> (html/html-resource "public/register.html")
         set-form-action
         add-anti-forgery
         (add-registration-errors err)
         (add-params params)
         (t/translate translator)
         html/emit*
         (apply str))))

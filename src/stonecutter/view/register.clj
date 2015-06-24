(ns stonecutter.view.register
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

(defn remove-element [enlive-m selector]
  (html/at enlive-m selector nil))

(def error-translations
  {:email {:invalid "content:registration-form/email-address-invalid-validation-message"
           :duplicate "content:registration-form/email-address-duplicate-validation-message"
           :too-long "content:registration-form/email-address-too-long-validation-message"}
   :password {:invalid "content:registration-form/password-invalid-validation-message"}
   :confirm-password {:invalid "content:registration-form/confirm-password-invalid-validation-message"}})

(defn add-email-error [enlive-m err]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--registration-email])
          (html/at [:.clj--registration-email__validation] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    (remove-element enlive-m [:.clj--registration-email__validation])))

(defn add-password-error [enlive-m err]
  (if (contains? err :password)
    (let [error-translation (get-in error-translations [:password (:password err)])]
      (-> enlive-m
          (add-error-class [:.clj--registration-password])
          (html/at [:.clj--registration-password__validation] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    (remove-element enlive-m [:.clj--registration-password__validation])))

(defn add-confirm-password-error [enlive-m err]
  (if (contains? err :confirm-password)
    (let [error-translation (get-in error-translations [:confirm-password (:confirm-password err)])]
      (-> enlive-m
          (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    (remove-element enlive-m [:.clj--validation-summary])))

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

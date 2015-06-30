(ns stonecutter.view.register
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(def error-translations
  {:email {:invalid "content:registration-form/email-address-invalid-validation-message"
           :duplicate "content:registration-form/email-address-duplicate-validation-message"
           :too-long "content:registration-form/email-address-too-long-validation-message"}
   :password {:blank "content:registration-form/password-blank-validation-message"
              :too-long "content:registration-form/password-too-long-validation-message"
              :too-short "content:registration-form/password-too-short-validation-message"}
   :confirm-password {:invalid "content:registration-form/confirm-password-invalid-validation-message"}})

(defn set-sign-in-link [enlive-m]
  (html/at enlive-m 
           [:.clj--sign-in__link] (html/set-attr :href (r/path :sign-in))))

(defn add-email-error [enlive-m err]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--registration-email])
          (html/at [:.clj--registration-email__validation] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--registration-email__validation])))

(defn add-password-error [enlive-m err]
  (if (contains? err :password)
    (let [error-translation (get-in error-translations [:password (:password err)])]
      (-> enlive-m
          (add-error-class [:.clj--registration-password])
          (html/at [:.clj--registration-password__validation] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--registration-password__validation])))

(defn add-confirm-password-error [enlive-m err]
  (if (contains? err :confirm-password)
    (let [error-translation (get-in error-translations [:confirm-password (:confirm-password err)])]
      (-> enlive-m
          (html/at [:.clj--validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:registration-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--validation-summary])))

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

(defn registration-form [request]
  (let [context (:context request)
        err (:errors context)
        translator (:translator context)
        params (:params request)]
    (->> (vh/load-template "public/register.html")
         set-sign-in-link
         set-form-action
         vh/add-anti-forgery
         (add-registration-errors err)
         (add-params params)
         (t/translate translator)
         html/emit*
         (apply str))))

(ns stonecutter.view.sign-in
  (:require [traduki.core :as t]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(def error-translations
  { :email {:invalid "content:sign-in-form/email-address-invalid-validation-message"
            :too-long "content:sign-in-form/email-address-too-long-validation-message"}
   :password {:blank "content:sign-in-form/password-blank-validation-message"
              :too-long "content:sign-in-form/password-too-long-validation-message"
              :too-short "content:sign-in-form/password-too-short-validation-message"}
   :credentials {:invalid "content:sign-in-form/invalid-credentials-validation-message"}})

(defn set-registration-link [enlive-m]
  (html/at enlive-m 
           [:.clj--register__link] (html/set-attr :href (r/path :show-registration-form))))

(defn add-email-error [enlive-m err]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-email])
          (html/at [:.clj--sign-in-email__validation] (html/set-attr :data-l8n (or error-translation "content:sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-email__validation])))

(defn add-password-error [enlive-m err]
  (if (contains? err :password)
    (let [error-translation (get-in error-translations [:password (:password err)])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-password])
          (html/at [:.clj--sign-in-password__validation] (html/set-attr :data-l8n (or error-translation "content:sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-password__validation])))

(defn add-invalid-credentials-error [enlive-m err]
  (if (contains? err :credentials)
    (let [error-translation (get-in error-translations [:credentials (:credentials err)])]
      (-> enlive-m
          (html/at [:.clj--validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--validation-summary])))

(defn add-sign-in-errors [err enlive-m]
  (-> enlive-m
      (add-email-error err)
      (add-password-error err)
      (add-invalid-credentials-error err)))

(defn add-params [params enlive-m]
  (html/at enlive-m
           [:.clj--email__input] (html/set-attr :value (:email params))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :sign-in))))

(defn sign-in-form [request]
  (let [context (:context request)
        params (:params request)
        err (:errors context)
        translator (:translator context)]
  (->> (vh/load-template "public/sign-in.html")
       set-registration-link
       set-form-action
       vh/add-anti-forgery
       (add-sign-in-errors err)
       (add-params params)
       (t/translate translator)
       html/emit*
       (apply str))))

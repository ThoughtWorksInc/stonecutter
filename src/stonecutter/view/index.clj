(ns stonecutter.view.index
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(def error-translations
  {:email {:invalid "content:sign-in-form/email-address-invalid-validation-message"
           :too-long "content:sign-in-form/email-address-too-long-validation-message"}
   :password {:blank "content:sign-in-form/password-blank-validation-message"
              :too-long "content:sign-in-form/password-too-long-validation-message"
              :too-short "content:sign-in-form/password-too-short-validation-message"}
   :credentials {:invalid "content:sign-in-form/invalid-credentials-validation-message"
                 :confirmation-invalid "content:confirmation-sign-in-form/invalid-credentials-validation-message"}})

(defn add-email-error [err enlive-m]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-email])
          (html/at [:.clj--sign-in-email__validation] (html/set-attr :data-l8n (or error-translation "content:sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-email__validation])))

(defn add-password-error [err enlive-m]
  (if (contains? err :password)
    (let [error-translation (get-in error-translations [:password (:password err)])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-password])
          (html/at [:.clj--sign-in-password__validation] (html/set-attr :data-l8n (or error-translation "content:sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-password__validation])))

(defn add-invalid-credentials-error [err enlive-m]
  (if (contains? err :credentials)
    (let [error-translation (get-in error-translations [:credentials (:credentials err)])]
      (-> enlive-m
          (html/at [:.clj--validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--validation-summary])))

(defn add-sign-in-errors [err enlive-m]
  (->> enlive-m
       (add-email-error err)
       (add-password-error err)
       (add-invalid-credentials-error err)))

(defn index [request]
  (let [error-m (get-in request [:context :errors])]
    (->> (vh/load-template "public/home.html")
         (vh/set-form-action [:.clj--register__form] (r/path :register-user))
         (vh/set-form-action [:.clj--sign-in__form] (r/path :sign-in))
         (vh/set-attribute [:.clj--forgot-password] :href (r/path :show-forgotten-password-form))
         (add-sign-in-errors error-m)
         vh/remove-work-in-progress)))


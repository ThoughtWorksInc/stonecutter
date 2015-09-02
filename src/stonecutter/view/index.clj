(ns stonecutter.view.index
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(def error-translations
  {:email {:invalid "content:index/sign-in-email-address-invalid-validation-message"
           :too-long "content:index/sign-in-email-address-too-long-validation-message"}
   :password {:blank "content:index/sign-in-password-blank-validation-message"
              :too-long "content:index/sign-in-password-too-long-validation-message"
              :too-short "content:index/sign-in-password-too-short-validation-message"}
   :credentials {:invalid "content:index/sign-in-invalid-credentials-validation-message"
                 :confirmation-invalid "content:confirmation-sign-in-form/invalid-credentials-validation-message"}})

(defn add-email-error [err enlive-m]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-email])
          (html/at [:.clj--sign-in-email__validation] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-email__validation])))

(defn add-password-error [err enlive-m]
  (if (contains? err :password)
    (let [error-translation (get-in error-translations [:password (:password err)])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-password])
          (html/at [:.clj--sign-in-password__validation] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-password__validation])))

(defn add-invalid-credentials-error [err enlive-m]
  (if (contains? err :credentials)
    (let [error-translation (get-in error-translations [:credentials (:credentials err)])]
      (-> enlive-m
          (html/at [:.clj--validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--validation-summary])))

(defn add-sign-in-errors [err enlive-m]
  (->> enlive-m
       (add-email-error err)
       (add-password-error err)
       (add-invalid-credentials-error err)))

(defn set-email-input [params enlive-m]
  (html/at enlive-m
           [:.clj--email__input] (html/set-attr :value (:email params))))

(defn index [request]
  (let [error-m (get-in request [:context :errors])]
    (->> (vh/load-template "public/home.html")
         (vh/set-form-action [:.clj--register__form] (r/path :register-user))
         (vh/set-form-action [:.clj--sign-in__form] (r/path :sign-in))
         (vh/set-attribute [:.clj--forgot-password] :href (r/path :show-forgotten-password-form))
         (add-sign-in-errors error-m)
         (set-email-input (:params request) )
         vh/remove-work-in-progress)))


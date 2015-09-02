(ns stonecutter.view.index
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(def error-translations
  {:sign-in-email                 {:invalid  "content:index/sign-in-email-address-invalid-validation-message"
                                   :too-long "content:index/sign-in-email-address-too-long-validation-message"}
   :sign-in-password              {:blank     "content:index/sign-in-password-blank-validation-message"
                                   :too-long  "content:index/sign-in-password-too-long-validation-message"
                                   :too-short "content:index/sign-in-password-too-short-validation-message"}
   :sign-in-credentials           {:invalid "content:index/sign-in-invalid-credentials-validation-message"}
   :registration-email            {:invalid   "content:index/register-email-address-invalid-validation-message"
                                   :duplicate "content:index/register-email-address-duplicate-validation-message"
                                   :too-long  "content:index/register-email-address-too-long-validation-message"}
   :registration-password         {:blank     "content:index/register-password-blank-validation-message"
                                   :too-long  "content:index/register-password-too-long-validation-message"
                                   :too-short "content:index/register-password-too-short-validation-message"}
   :registration-confirm-password {:invalid "content:index/register-confirm-password-invalid-validation-message"}})

(defn add-sign-in-email-error [err enlive-m]
  (if-let [sign-in-email-error (:sign-in-email err)]
    (let [error-translation (get-in error-translations [:sign-in-email sign-in-email-error])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-email])
          (html/at [:.clj--sign-in-email__validation] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-email__validation])))

(defn add-sign-in-password-error [err enlive-m]
  (if-let [sign-in-password-error (:sign-in-password err)]
    (let [error-translation (get-in error-translations [:sign-in-password sign-in-password-error])]
      (-> enlive-m
          (add-error-class [:.clj--sign-in-password])
          (html/at [:.clj--sign-in-password__validation] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-password__validation])))

(defn add-invalid-credentials-error [err enlive-m]
  (if-let [sign-in-credentials-error (:sign-in-credentials err)]
    (let [error-translation (get-in error-translations [:sign-in-credentials sign-in-credentials-error])]
      (-> enlive-m
          (html/at [:.clj--sign-in-validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--sign-in-validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-validation-summary])))

(defn add-sign-in-errors [err enlive-m]
  (->> enlive-m
       (add-sign-in-email-error err)
       (add-sign-in-password-error err)
       (add-invalid-credentials-error err)))

(defn set-sign-in-email-input [params enlive-m]
  (html/at enlive-m
           [:.clj--sign-in-email__input] (html/set-attr :value (:sign-in-email params))))

(defn set-registration-email-input [params enlive-m]
  (html/at enlive-m
           [:.clj--registration-email__input] (html/set-attr :value (:registration-email params))))


(defn add-registration-email-error [enlive-m err]
  (if-let [registration-email-error (:registration-email err)]
    (let [error-translation (get-in error-translations [:registration-email registration-email-error])]
      (-> enlive-m
          (add-error-class [:.clj--registration-email])
          (html/at [:.clj--registration-email__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--registration-email__validation])))

(defn add-registration-password-error [enlive-m err]
  (if-let [registration-password-error (:registration-password err)]
    (let [error-translation (get-in error-translations [:registration-password registration-password-error])]
      (-> enlive-m
          (add-error-class [:.clj--registration-password])
          (html/at [:.clj--registration-password__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--registration-password__validation])))

(defn add-registration-confirm-password-error [enlive-m err]
  (if-let [registration-confirm-password-error (:registration-confirm-password err)]
    (let [error-translation (get-in error-translations [:registration-confirm-password registration-confirm-password-error])]
      (-> enlive-m
          (html/at [:.clj--registration-validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--registration-validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--registration-validation-summary])))

(defn add-registration-errors [err enlive-m]
  (-> enlive-m
      (add-registration-email-error err)
      (add-registration-password-error err)
      (add-registration-confirm-password-error err)))

(defn index [request]
  (let [error-m (get-in request [:context :errors])]
    (->> (vh/load-template "public/index.html")
         (vh/set-form-action [:.clj--register__form] (r/path :register-user))
         (vh/set-form-action [:.clj--sign-in__form] (r/path :sign-in))
         (vh/set-attribute [:.clj--forgot-password] :href (r/path :show-forgotten-password-form))
         (add-sign-in-errors error-m)
         (add-registration-errors error-m)
         (set-sign-in-email-input (:params request))
         (set-registration-email-input (:params request))
         vh/remove-work-in-progress
         vh/add-anti-forgery)))


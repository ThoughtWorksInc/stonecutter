(ns stonecutter.view.index
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(def unknown-error-translation-key "content:index/register-unknown-error")

(def error-translations
  {:sign-in-email           {:invalid  "content:index/sign-in-email-address-invalid-validation-message"
                             :too-long "content:index/sign-in-email-address-too-long-validation-message"}
   :sign-in-password        {:blank     "content:index/sign-in-password-blank-validation-message"
                             :too-long  "content:index/sign-in-password-too-long-validation-message"
                             :too-short "content:index/sign-in-password-too-short-validation-message"}
   :sign-in-credentials     {:invalid "content:index/sign-in-invalid-credentials-validation-message"}
   :registration-first-name {:blank    "content:index/register-first-name-blank-validation-message"
                             :too-long "content:index/register-first-name-too-long-validation-message"}
   :registration-last-name  {:blank    "content:index/register-last-name-blank-validation-message"
                             :too-long "content:index/register-last-name-too-long-validation-message"}
   :registration-email      {:invalid   "content:index/register-email-address-invalid-validation-message"
                             :duplicate "content:index/register-email-address-duplicate-validation-message"
                             :too-long  "content:index/register-email-address-too-long-validation-message"}
   :registration-password   {:blank     "content:index/register-password-blank-validation-message"
                             :too-long  "content:index/register-password-too-long-validation-message"
                             :too-short "content:index/register-password-too-short-validation-message"}})

(defn add-sign-in-email-error [err enlive-m]
  (if-let [sign-in-email-error (:sign-in-email err)]
    (let [error-translation (get-in error-translations [:sign-in-email sign-in-email-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--sign-in-email])
          (html/at [:.clj--sign-in-email__validation] (html/set-attr :data-l8n (or error-translation "content:index/sign-in-unknown-error")))))
    (vh/remove-element enlive-m [:.clj--sign-in-email__validation])))

(defn add-sign-in-password-error [err enlive-m]
  (if-let [sign-in-password-error (:sign-in-password err)]
    (let [error-translation (get-in error-translations [:sign-in-password sign-in-password-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--sign-in-password])
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

(defn set-registration-inputs [params enlive-m]
  (html/at enlive-m
           [:.clj--registration-email__input] (html/set-attr :value (:registration-email params))
           [:.clj--registration-first-name__input] (html/set-attr :value (:registration-first-name params))
           [:.clj--registration-last-name__input] (html/set-attr :value (:registration-last-name params))))

(defn add-registration-first-name-error [enlive-m err]
  (if-let [registration-first-name-error (:registration-first-name err)]
    (let [error-translation (get-in error-translations [:registration-first-name registration-first-name-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--registration-first-name])
          (html/at [:.clj--registration-first-name__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    enlive-m))

(defn add-registration-last-name-error [enlive-m err]
  (if-let [registration-last-name-error (:registration-last-name err)]
    (let [error-translation (get-in error-translations [:registration-last-name registration-last-name-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--registration-last-name])
          (html/at [:.clj--registration-last-name__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    enlive-m))

(defn add-registration-email-error [enlive-m err]
  (if-let [registration-email-error (:registration-email err)]
    (let [error-translation (get-in error-translations [:registration-email registration-email-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--registration-email])
          (html/at [:.clj--registration-email__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    enlive-m))

(defn add-registration-password-error [enlive-m err]
  (if-let [registration-password-error (:registration-password err)]
    (let [error-translation (get-in error-translations [:registration-password registration-password-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--registration-password])
          (html/at [:.clj--registration-password__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    enlive-m))

(def registration-error-keys-ordered [:registration-first-name
                                      :registration-last-name
                                      :registration-email
                                      :registration-password])

(defn get-kv-for-key [a-map a-key]
  (when-let [v (a-key a-map)]
    [a-key v]))

(defn kv-pairs-from-map-ordered-by [the-map ordered-keys]
  (remove nil? (map #(get-kv-for-key the-map %) ordered-keys)))


(defn add-registration-validation-summary [enlive-m err]
  (if (empty? (select-keys err registration-error-keys-ordered))
    (vh/remove-element enlive-m [:.clj--registration-validation-summary])
    (let [ordered-error-key-pairs (kv-pairs-from-map-ordered-by err registration-error-keys-ordered)]
      (-> enlive-m
          (html/at [:.clj--registration-validation-summary__item]
                   (html/clone-for [error-key-pair ordered-error-key-pairs]
                                   (html/set-attr :data-l8n (or (get-in error-translations error-key-pair)
                                                                unknown-error-translation-key))))))))

(defn add-registration-errors [err enlive-m]
  (-> enlive-m
      (add-registration-validation-summary err)
      (add-registration-first-name-error err)
      (add-registration-last-name-error err)
      (add-registration-email-error err)
      (add-registration-password-error err)))

(defn index [request]
  (let [error-m (get-in request [:context :errors])]
    (->> (vh/load-template "public/index.html")
         (vh/set-form-action [:.clj--register__form] (r/path :sign-in-or-register))
         (vh/set-form-action [:.clj--sign-in__form] (r/path :sign-in-or-register))
         (vh/set-attribute [:.clj--forgot-password] :href (r/path :show-forgotten-password-form))
         (add-sign-in-errors error-m)
         (add-registration-errors error-m)
         (set-sign-in-email-input (:params request))
         (set-registration-inputs (:params request))
         vh/remove-work-in-progress
         vh/add-anti-forgery
         (vh/add-script "js/main.js"))))

(defn remove-sign-in-elements [enlive-m]
  (vh/remove-element enlive-m [:.clj--sign-in__form]))

(defn set-body-class-to-accept-invite [enlive-m]
  (-> (html/at enlive-m [:.func--index-page] (html/add-class "func--accept-invite-page"))
      (html/at [:.func--accept-invite-page] (html/remove-class "func--index-page"))))

(defn accept-invite [request]
  (let [error-m (get-in request [:context :errors])]
    (->> (vh/load-template "public/index.html")
         (vh/set-form-action [:.clj--register__form] (r/path :sign-in-or-register))
         (add-registration-errors error-m)
         (set-registration-inputs (:params request))
         (remove-sign-in-elements)
         set-body-class-to-accept-invite
         vh/remove-work-in-progress
         vh/add-anti-forgery
         (vh/add-script "js/main.js")))
  )
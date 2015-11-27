(ns stonecutter.view.forgotten-password
  (:require [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(def error-translations
  {:email {:invalid "content:forgot-password/email-address-invalid-validation-message"
           :too-long "content:forgot-password/email-address-too-long-validation-message"
           :non-existent "content:forgot-password/email-address-non-existent-validation-message"}})

(defn set-form-action [enlive-m]
  (-> enlive-m
      (html/at [:.clj--forgotten-password__form] (html/do-> (html/set-attr :action (r/path :send-forgotten-password-email))
                                                            (html/set-attr :method "post")))))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--invalid")))

(defn add-email-error [err enlive-m]
  (if (contains? err :email)
    (let [error-translation (get-in error-translations [:email (:email err)])]
      (-> enlive-m
          (add-error-class [:.clj--forgotten-password-email])
          (html/at [:.clj--forgotten-password-email__validation] (html/set-attr :data-l8n (or error-translation "content:forgot-password/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--forgotten-password-email__validation])))

(defn set-email-input [email enlive-m]
  (html/at enlive-m
           [:.clj--forgotten-password-email__input] (html/set-attr :value email)))

(defn forgotten-password-form [request]
  (->> (vh/load-template-with-lang "public/forgot-password.html" request)
       set-form-action
       (add-email-error (get-in request [:context :errors]))
       (set-email-input (get-in request [:params :email]))
       (vh/set-flash-message request :expired-password-reset)
       vh/add-anti-forgery))

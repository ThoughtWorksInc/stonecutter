(ns stonecutter.view.change-email
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(def form-row-error-css-class "form-row--invalid")

(def error-translations
  {:new-email {:blank     "content:change-email-form/new-email-blank-validation-message"
               :duplicate "content:change-email-form/new-email-duplicate-validation-message"
               :invalid   "content:change-email-form/new-email-invalid-validation-message"
               :unchanged "content:change-email-form/new-email-unchanged-validation-message"}})

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class form-row-error-css-class)))

(defn add-change-email-error [enlive-m err]
  (if-let [change-email-error (:new-email err)]
    (let [error-translation (get-in error-translations [:new-email change-email-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--email-address])
          (html/at [:.clj--new-email__validation] (html/set-attr :data-l8n (or error-translation "content:change-email-form/change-email-unknown-error")))))
    enlive-m))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m [:.clj--change-email-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn add-change-email-errors [enlive-m err]
  (if (empty? err)
    enlive-m
    (add-change-email-error enlive-m err)))

(defn change-email-form [request]
  (let [err (get-in request [:context :errors])
        library-m (vh/load-template-with-lang "public/library.html" request)]
    (-> (vh/load-template-with-lang "public/change-email.html" request)
        (vh/display-admin-navigation-links request library-m)
        (add-change-email-errors err)
        (vh/set-form-action [:.clj--change-email__form] (r/path :change-email))
        set-cancel-link
        vh/add-anti-forgery
        vh/remove-work-in-progress)))


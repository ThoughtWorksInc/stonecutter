(ns stonecutter.view.change-email
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(def unknown-error-translation-key
  "content:change-password-form/unknown-error")

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
          (vh/add-error-class [:.clj--new-email__validation])
          (html/at [:.clj--new-email__validation] (html/set-attr :data-l8n (or error-translation "content:index/register-unknown-error")))))
    enlive-m))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m [:.clj--change-email-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--change-email__form] (html/set-attr :action (r/path :change-email))))

(defn add-change-email-errors [enlive-m err]
  (if (empty? err)
    (html/at enlive-m [:.clj--validation-summary] (html/remove-class "validation-summary--show"))
    (do (add-change-email-error enlive-m err)
        )))

(defn change-email-form [request]
  (let [err (get-in request [:context :errors])
        library-m (vh/load-template "public/library.html")]
    (-> (vh/load-template "public/change-email.html")
        set-form-action
        set-cancel-link
        (vh/display-admin-navigation-links request library-m)
        (add-change-email-errors err)
        vh/add-anti-forgery
        vh/remove-work-in-progress
        #_(vh/add-script "js/main.js"))))


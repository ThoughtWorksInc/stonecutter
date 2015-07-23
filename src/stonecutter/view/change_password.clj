(ns stonecutter.view.change-password
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(def current-password-error-translation-key
   "content:change-password-form/current-password-invalid-validation-message")

(def unknown-error-translation-key
   "content:change-password-form/unknown-error")

(def error-translations
  {:current-password {:invalid   current-password-error-translation-key
                      :blank     current-password-error-translation-key
                      :too-short current-password-error-translation-key
                      :too-long  current-password-error-translation-key}
   :new-password {:blank "content:change-password-form/new-password-blank-validation-message"
                  :too-short "content:change-password-form/new-password-too-short-validation-message"
                  :too-long "content:change-password-form/new-password-too-long-validation-message"
                  :unchanged "content:change-password-form/new-password-unchanged-validation-message"}
   :confirm-new-password {:invalid "content:change-password-form/confirm-new-password-invalid-validation-message"}})

(defn add-error [enlive-m err]
  (if (empty? err)
    (vh/remove-element enlive-m [:.clj--validation-summary])
    (let [error-key-pair (first err)
          error-translation (get-in error-translations error-key-pair)]
      (-> enlive-m
          (html/at [:.clj--validation-summary]
                   (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item]
                   (html/set-attr :data-l8n (or error-translation unknown-error-translation-key)))))))

(defn add-change-password-errors [err enlive-m]
  (-> enlive-m
      (add-error err)
      ))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--change-password__form] (html/set-attr :action (r/path :change-password))))

(defn change-password-form [request]
  (let [err (get-in request [:context :errors])]
    (->> (vh/load-template "public/change-password.html")
         set-form-action
         set-cancel-link
         (add-change-password-errors err)
         vh/add-anti-forgery
         vh/remove-work-in-progress)))

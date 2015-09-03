(ns stonecutter.view.confirmation-sign-in
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(def error-translations
  {:credentials {:confirmation-invalid "content:confirmation-sign-in-form/invalid-credentials-validation-message"}})

(defn add-invalid-credentials-error [err enlive-m]
  (if (contains? err :credentials)
    (let [error-translation (get-in error-translations [:credentials (:credentials err)])]
      (-> enlive-m
          (html/at [:.clj--validation-summary] (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n (or error-translation "content:confirmation-sign-in-form/unknown-error")))))
    (vh/remove-element enlive-m [:.clj--validation-summary])))

(defn set-confirmation-id [params enlive-m]
  (html/at enlive-m
           [:.clj--confirmation-id__input] (html/set-attr :value (:confirmation-id params))))

(defn set-form-action [path enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action path)))

(defn set-forgotten-password-link [enlive-m]
  (html/at enlive-m [:.clj--forgot-password] (html/set-attr :href (r/path :show-forgotten-password-form))))

(defn confirmation-sign-in-form [request]
  (->> (vh/load-template "public/confirmation-sign-in.html")
       (set-form-action (r/path :confirmation-sign-in))
       set-forgotten-password-link
       (set-confirmation-id (:params request))
       (add-invalid-credentials-error (get-in request [:context :errors]))
       vh/add-anti-forgery))

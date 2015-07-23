(ns stonecutter.view.change-password
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))
(defn add-current-password-error [enlive-m err]
  (if (contains? err :current-password)
    (-> enlive-m
        (html/at [:.clj--validation-summary] (html/add-class "validation-summary--show"))
        (html/at [:.clj--validation-summary__item] (html/set-attr :data-l8n "content:change-password-form/current-password-invalid-validation-message")))
    (vh/remove-element enlive-m [:.clj--validation-summary])))

(defn add-change-password-errors [err enlive-m]
  (-> enlive-m
      (add-current-password-error err)))

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

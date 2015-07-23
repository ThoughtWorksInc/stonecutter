(ns stonecutter.view.change-password
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--change-password__form] (html/set-attr :action (r/path :change-password))))

(defn change-password-form [request]
  (->> (vh/load-template "public/change-password.html")
       set-form-action
       set-cancel-link
       vh/add-anti-forgery
       vh/remove-work-in-progress))

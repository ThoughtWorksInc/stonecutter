(ns stonecutter.view.delete-account
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m
           [:.clj--delete-account-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--delete-account__form] (html/set-attr :action (r/path :delete-account))))

(defn delete-account-confirmation [request]
  (->> (vh/load-template "public/delete-account.html")
       set-form-action
       set-cancel-link
       vh/add-anti-forgery
       vh/remove-work-in-progress))

(defn profile-deleted [request]
  (->> (vh/load-template "public/profile-deleted.html")
       vh/remove-work-in-progress))

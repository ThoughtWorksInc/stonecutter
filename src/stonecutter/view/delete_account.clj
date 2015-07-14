(ns stonecutter.view.delete-account
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj-delete-account-form] (html/set-attr :action (r/path :delete-account))))

(defn delete-account-confirmation [request]
  (->> (vh/load-template "public/delete-account.html")
       set-form-action
       vh/add-anti-forgery
       vh/remove-work-in-progress))

(defn profile-deleted [request]
  (->> (vh/load-template "public/profile-deleted.html")
       vh/remove-work-in-progress))

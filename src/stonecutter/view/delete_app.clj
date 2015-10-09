(ns stonecutter.view.delete-app
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-form-action [path enlive-m]
  (html/at enlive-m [:.clj--delete-app__form] (html/set-attr :action path)))

(defn set-cancel-link [path enlive-m]
  (html/at enlive-m [:.clj--delete-app-cancel__link] (html/set-attr :href path)))

(defn delete-app-confirmation [request]
  (->> (vh/load-template "public/delete-app.html")
       (set-form-action (r/path :delete-app :app-id (get-in request [:params :app-id])))
       (set-cancel-link (r/path :show-apps-list))
       vh/add-anti-forgery
       vh/remove-work-in-progress))

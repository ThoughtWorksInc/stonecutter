(ns stonecutter.view.apps-list
  (:require [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(defn apps-list-items [clients enlive-m]
  (let [admin-app-item-snippet (first (html/select enlive-m [:.clj--admin-app-item]))]
    (html/at admin-app-item-snippet [html/root]
             (html/clone-for [client clients]
                             [:.clj--admin-app-item__title] (html/content (:name client))
                             [:.clj--client-id] (html/content (:client-id client))
                             [:.clj--client-secret] (html/content (:client-secret client))
                             [:.clj--client-url] (html/content (:url client))))))

(defn add-apps-list [clients enlive-m]
  (if-not (empty? clients)
    (html/at enlive-m [:.clj--admin-apps-list] (html/content (apps-list-items clients enlive-m)))
    (html/at enlive-m [:.clj--admin-apps-list] (html/content nil))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :create-client))))

(defn apps-list [request]
  (let [clients (get-in request [:context :clients])]
    (->> (vh/load-template "public/admin-apps.html")
         (vh/add-script "js/main.js")
         vh/remove-work-in-progress
         vh/set-sign-out-link
         vh/set-apps-list-link
         vh/set-user-list-link
         set-form-action
         vh/add-anti-forgery
         (add-apps-list clients))))
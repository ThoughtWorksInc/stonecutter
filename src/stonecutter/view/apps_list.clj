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


(defn set-flash-message [request enlive-m]
  (if-let [new-client-name (get-in request [:flash :name])]
    (html/at enlive-m [:.clj--new-app-name] (html/content new-client-name))
    (vh/remove-element enlive-m [:.clj--flash-message-container])))

(defn apps-list [request]
  (let [clients (get-in request [:context :clients])]
    (->> (vh/load-template "public/admin-apps.html")
         vh/remove-work-in-progress
         vh/set-admin-links
         set-form-action
         vh/add-anti-forgery
         (set-flash-message request)
         (add-apps-list clients))))
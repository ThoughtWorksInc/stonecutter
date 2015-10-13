(ns stonecutter.view.apps-list
  (:require [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(def error-translations
  {:app-name {:blank "content:admin-app-list/app-name-blank-error"}
   :app-url  {:blank "content:admin-app-list/app-url-blank-error"}})

(defn delete-app-route [client]
  (r/path :delete-app-confirmation :app-id (or (:client-id client) "unknown")))

(defn apps-list-items [clients enlive-m]
  (let [admin-app-item-snippet (first (html/select enlive-m [:.clj--admin-app-item]))]
    (html/at admin-app-item-snippet [html/root]
             (html/clone-for [client clients]
                             [:.clj--admin-app-item__title] (html/content (:name client))
                             [:.clj--client-id] (html/content (:client-id client))
                             [:.clj--client-secret] (html/content (:client-secret client))
                             [:.clj--client-url] (html/content (:url client))
                             [:.clj--delete-app__link] (html/set-attr :href (delete-app-route client))))))

(defn add-apps-list [clients enlive-m]
  (if-not (empty? clients)
    (html/at enlive-m [:.clj--admin-apps-list] (html/content (apps-list-items clients enlive-m)))
    (html/at enlive-m [:.clj--admin-apps-list] (html/content nil))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :create-client))))

(defn set-add-app-message [enlive-m new-client-name]
  (if new-client-name
    (html/at enlive-m [:.clj--new-app-name] (html/content new-client-name))
    (vh/remove-element enlive-m [:.func--flash-message-add-container])))

(defn set-deleted-app-message [enlive-m deleted-app-name]
  (if deleted-app-name
    (html/at enlive-m [:.clj--deleted-app-name] (html/content deleted-app-name))
    (vh/remove-element enlive-m [:.func--flash-message-delete-container])))

(defn set-flash-message [request enlive-m]
  (let [new-client-name (get-in request [:flash :added-app-name])
        deleted-app-name (get-in request [:flash :deleted-app-name])]
    (-> enlive-m
        (set-deleted-app-message deleted-app-name)
        (set-add-app-message new-client-name))))

(defn add-app-name-blank-error [err enlive-m]
  (if-let [app-name-error (:app-name err)]
    (let [error-translation (get-in error-translations [:app-name app-name-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--application-name])
          (html/at [:.clj--application-name__validation] (html/set-attr :data-l8n (or error-translation "content:admin-app-list/unknown-error")))))
    enlive-m))

(defn add-app-url-blank-error [err enlive-m]
  (if-let [app-url-error (:app-url err)]
    (let [error-translation (get-in error-translations [:app-url app-url-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--application-url])
          (html/at [:.clj--application-url__validation] (html/set-attr :data-l8n (or error-translation "content:admin-app-list/unknown-error")))))
    enlive-m))

(defn apps-list [request]
  (let [clients (get-in request [:context :clients])
        error   (get-in request [:context :errors])]
    (->> (vh/load-template "public/admin-apps.html")
         vh/remove-work-in-progress
         vh/set-admin-links
         (add-app-name-blank-error error)
         (add-app-url-blank-error error)
         set-form-action
         vh/add-anti-forgery
         (set-flash-message request)
         (add-apps-list clients))))
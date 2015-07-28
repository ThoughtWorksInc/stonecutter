(ns stonecutter.view.unshare-profile-card
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-client-id [client-id enlive-m]
  (html/at enlive-m
           [:.clj--client-id__input] (html/set-attr :value client-id)))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m
           [:.clj--unshare-profile-card-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m
           [:.clj--unshare-profile-card__form] (html/set-attr :action (r/path :unshare-profile-card))))

(defn set-client-name [client-name enlive-m]
  (html/at enlive-m
           [:.clj--client-name] (html/content client-name)))

(defn unshare-profile-card [request]
  (let [client-id (get-in request [:context :client :client-id])
        client-name (get-in request [:context :client :name])]
    (->> (vh/load-template "public/unshare-profile-card.html")
         (set-client-name client-name)
         set-form-action
         (set-client-id client-id)
         set-cancel-link
         vh/add-anti-forgery)))

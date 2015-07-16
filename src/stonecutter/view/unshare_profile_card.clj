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
  (html/at enlive-m [:.clj--unshare-profile-card__form] (html/set-attr :action (r/path :unshare-profile-card))))

(defn unshare-profile-card [request]
  (let [client-id (get-in request [:context :client-id])]
    (->> (vh/load-template "public/unshare-profile-card.html")
         set-form-action
         (set-client-id client-id)
         set-cancel-link
         vh/add-anti-forgery)))

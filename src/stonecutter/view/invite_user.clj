(ns stonecutter.view.invite-user
  (:require [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]
            [net.cgrand.enlive-html :as html]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :send-invite))))

(defn set-flash-message [enlive-m request]
  (let [email-address (get-in request [:flash :email-address])]
    (if email-address
      (html/at enlive-m [:.clj--invited-email] (html/content email-address))
      (vh/remove-element enlive-m [:.func--flash-message-container]))))

(defn invite-user [request]
  (-> (vh/load-template "public/admin-invite-user.html")
      vh/remove-work-in-progress
      vh/set-admin-links
      set-form-action
      (set-flash-message request)
      vh/add-anti-forgery))

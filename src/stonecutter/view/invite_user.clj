(ns stonecutter.view.invite-user
  (:require [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]
            [net.cgrand.enlive-html :as html]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :send-invite))))

(defn invite-user [request]
  (-> (vh/load-template "public/admin-invite-user.html")
      vh/remove-work-in-progress
      vh/set-admin-links
      set-form-action
      vh/add-anti-forgery))

(ns stonecutter.view.invite-user
  (:require [stonecutter.view.view-helpers :as vh]))

(defn invite-user [request]
  (-> (vh/load-template "public/admin-invite-user.html")
      vh/remove-work-in-progress
      vh/set-admin-links
      vh/add-anti-forgery))

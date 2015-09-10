(ns stonecutter.controller.admin
  (:require [stonecutter.helper :as sh]
            [stonecutter.view.user-list :as user-list]
            [stonecutter.db.user :as u]))

(defn show-user-list [user-store request]
  (sh/enlive-response (user-list/user-list request) (:context request)))

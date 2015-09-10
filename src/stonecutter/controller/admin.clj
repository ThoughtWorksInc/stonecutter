(ns stonecutter.controller.admin
  (:require [stonecutter.helper :as sh]
            [stonecutter.view.user-list :as user-list]
            [stonecutter.db.user :as u]))

(defn show-user-list [user-store request]
  (let [users (u/retrieve-users user-store)]
    (sh/enlive-response (-> request
                            (assoc-in [:context :users] users)
                            (user-list/user-list))
                        (:context request))))

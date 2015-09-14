(ns stonecutter.controller.admin
  (:require [ring.util.response :as r]
            [stonecutter.helper :as sh]
            [stonecutter.view.user-list :as user-list]
            [stonecutter.db.user :as u]
            [stonecutter.routes :as routes]
            [stonecutter.config :as config]))

(defn show-user-list [user-store request]
  (let [users (u/retrieve-users user-store)]
    (sh/enlive-response (-> request
                            (assoc-in [:context :users] users)
                            (user-list/user-list))
                        (:context request))))

(defn set-user-trustworthiness [user-store request]
  (let [email (get-in request [:params :login])
        trusted? (get-in request [:params :trust-toggle])]
    (if trusted?
      (u/update-user-role! user-store email (:trusted config/roles))
      (u/update-user-role! user-store email (:untrusted config/roles)))
    (r/redirect (routes/path :show-user-list))))

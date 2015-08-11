(ns stonecutter.controller.common
  (:require [ring.util.response :as response]
            [stonecutter.routes :as r]
            [stonecutter.db.token :as token]
            ))

(defn sign-in-user
  ([token-store user]
   (sign-in-user token-store user (r/path :home) {}))
  ([token-store user path]
   (sign-in-user token-store user path {}))
  ([token-store user path existing-session]
   (-> (response/redirect path)
       (assoc :session existing-session)
       (assoc-in [:session :user-login] (:login user))
       (assoc-in [:session :access_token] (token/generate-login-access-token token-store user)))))

(defn signed-in? [request]
  (let [session (:session request)]
    (and (:user-login session) (:access_token session))))

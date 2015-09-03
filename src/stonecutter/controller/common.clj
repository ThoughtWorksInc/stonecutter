(ns stonecutter.controller.common
  (:require [ring.util.response :as response]
            [stonecutter.routes :as r]
            [stonecutter.db.token :as token]
            ))

(defn sign-in-user
  ([response token-store user]
   (sign-in-user response token-store user {}))
  ([response token-store user existing-session]
   (-> response
       (assoc :session existing-session)
       (assoc-in [:session :user-login] (:login user))
       (assoc-in [:session :access_token] (token/generate-login-access-token token-store user)))))

(defn sign-in-to-index
  ([token-store user]
   (sign-in-to-index token-store user {}))
  ([token-store user existing-session]
   (-> (response/redirect (r/path :index))
       (sign-in-user token-store user existing-session))))

(defn signed-in? [request]
  (let [session (:session request)]
    (and (:user-login session) (:access_token session))))

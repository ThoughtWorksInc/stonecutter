(ns stonecutter.controller.common
  (:require [ring.util.response :as response]
            [stonecutter.routes :as r]
            [stonecutter.db.token :as token]
            [stonecutter.session :as session]))

(defn sign-in-user
  ([response token-store user]
   (sign-in-user response token-store user {}))
  ([response token-store user existing-session]
   (-> response
       (session/replace-session-with existing-session)
       (session/set-user-login (:login user))
       (session/set-access-token (token/generate-login-access-token token-store user)))))

(defn sign-in-to-index
  ([token-store user]
   (sign-in-to-index token-store user {}))
  ([token-store user existing-session]
   (-> (response/redirect (r/path :index))
       (sign-in-user token-store user existing-session))))

(defn signed-in? [request]
  (and (session/request->user-login request) (session/request->access-token request)))

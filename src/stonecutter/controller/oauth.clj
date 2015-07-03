(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as ep]
            [cheshire.core :as json]
            [stonecutter.routes :refer [path]]
            [stonecutter.storage :as s]))

(def auth-handler (ep/authorization-handler {:auto-approver (constantly true)
                                             :user-session-required-redirect (path :show-sign-in-form)}))

(def token-handler (ep/token-handler))

(defn authorise [request]
  (let [user (get-in request [:session :user])
        client-id (get-in request [:params :client_id])
        response (auth-handler request)]
    (-> response
        (assoc-in [:session :client-id] client-id)
        (assoc-in [:session :user] user))))

(defn validate-token [request]
  (let [auth-code (get-in request [:params :code])
        user-email (-> auth-code s/retrieve-user-with-auth-code :login)
        response (token-handler request)
        body (-> response
                 :body
                 (json/parse-string keyword)
                 (assoc :user-email user-email)
                 (json/generate-string))]
    (-> response
        (assoc :body body)
        (assoc-in [:session :user :email] user-email))))

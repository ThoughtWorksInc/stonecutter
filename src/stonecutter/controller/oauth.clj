(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as ep]
            [cheshire.core :as json]
            [stonecutter.routes :refer [path]]))

(def auth-handler (ep/authorization-handler {:auto-approver (constantly true)
                                             :user-session-required-redirect (path :show-sign-in-form)}))

(defn authorise [request]
  (let [user (get-in request [:session :user])
        client-id (get-in request [:params :client_id])
        response (auth-handler request)]
    (-> response
        (assoc-in [:session :client-id] client-id)
        (assoc-in [:session :user] user))))

(defn validate-token [request]
  (let [user-email (get-in request [:session :user :email])
        response ((ep/token-handler) request)
        body (-> response
                 :body
                 (json/parse-string keyword)
                 (assoc :user-email user-email)
                 (json/generate-string))]
    (assoc response :body body)))

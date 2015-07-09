(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as ep]
            [cheshire.core :as json]
            [ring.util.response :as r]
            [stonecutter.routes :refer [path]]
            [stonecutter.storage :as s]
            [stonecutter.controller.user :as user]
            [stonecutter.helper :refer :all]))

(defn authorisation-form []
  (fn [req] (user/show-authorise-form req)))

(def auth-handler (ep/authorization-handler {:auto-approver                  (constantly false)
                                             :user-session-required-redirect (path :show-sign-in-form)
                                             :authorization-form             (authorisation-form)}))

(def token-handler (ep/token-handler))

(defn authorise-client [request]
  (let [request (-> request
                    (assoc-in [:session :csrf-token] "token")
                    (assoc-in [:params :csrf-token] "token"))

        response (auth-handler request)]
    response))

(defn authorise [request]
  (let [user (get-in request [:session :user])
        access-token (get-in request [:session :access_token])
        client-id (get-in request [:params :client_id])
        response (auth-handler request)]
    (-> response
        (assoc-in [:session :client-id] client-id)
        (assoc-in [:session :user] user)
        (assoc-in [:session :access_token] access-token))))

(defn validate-token [request]
  (let [auth-code (get-in request [:params :code])
        user (s/retrieve-user-with-auth-code auth-code)
        user-email (:login user)
        user-id (:uid user)
        response (token-handler request)
        body (-> response
                 :body
                 (json/parse-string keyword)
                 (assoc :user-email user-email)
                 (assoc :user-id user-id)
                 (json/generate-string))]
    (-> response
        (assoc :body body)
        (assoc-in [:session :user :email] user-email))))

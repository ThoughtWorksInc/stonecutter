(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as cl-ep]
            [cheshire.core :as json]
            [stonecutter.routes :as routes]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as client]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.view.authorise-failure :as authorise-failure]
            [stonecutter.helper :as sh]))

(defn show-authorise-form [request]
  (sh/enlive-response (authorise/authorise-form request) (:context request)))

(defn add-error-to-uri [uri]
  (str uri "?error=access_denied"))

(defn show-authorise-failure [request]
  (let [client-id (get-in request [:session :client-id])
        client (client/retrieve-client client-id)
        client-name (:name client)
        redirect-uri (get-in request [:session :redirect-uri])
        callback-uri-with-error (add-error-to-uri redirect-uri)
        request (-> request
                     (assoc-in [:session :client-name] client-name)
                     (assoc-in [:params :callback-uri-with-error] callback-uri-with-error))]

    (sh/enlive-response (authorise-failure/show-authorise-failure request) (:context request))))


(defn auto-approver [request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user :login])
        user (user/retrieve-user user-email)
        authorised-clients (set (:authorised-clients user))]
    (boolean (authorised-clients client-id))))

(def auth-handler (cl-ep/authorization-handler {:auto-approver                  auto-approver
                                                :user-session-required-redirect (routes/path :show-sign-in-form)
                                                :authorization-form             show-authorise-form}))

(def token-handler (cl-ep/token-handler))

(defn authorise-client [request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user :login])
        response (auth-handler request)]
    (user/add-authorised-client-for-user! user-email client-id)
    response))

(defn authorise [request]
  (let [user (get-in request [:session :user])
        request (update-in request [:session] dissoc :csrf-token)
        access-token (get-in request [:session :access_token])
        client-id (get-in request [:params :client_id])
        redirect-uri (get-in request [:params :redirect_uri])
        response (auth-handler (assoc-in request [:headers "accept"] "text/html"))]
    (-> response
        (assoc-in [:session :client-id] client-id)
        (assoc-in [:session :user] user)
        (assoc-in [:session :redirect-uri] redirect-uri)
        (assoc-in [:session :access_token] access-token))))

(defn validate-token [request]
  (let [auth-code (get-in request [:params :code])
        user (user/retrieve-user-with-auth-code auth-code)
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
        (assoc-in [:session :user] user))))

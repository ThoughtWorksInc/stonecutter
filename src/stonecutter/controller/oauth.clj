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
  (let [client-id (get-in request [:params :client_id])]
    (if-let [client (client/retrieve-client client-id)]
      (let [context (assoc (:context request) :client client)]
        (-> (assoc request :context context)
            authorise/authorise-form
            (sh/enlive-response context)))
      {:status 404})))

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
        user-email (get-in request [:session :user-login])]
    (user/is-authorised-client-for-user? user-email client-id)))

(def auth-handler (cl-ep/authorization-handler {:auto-approver                  auto-approver
                                                :user-session-required-redirect (routes/path :show-sign-in-form)
                                                :authorization-form             show-authorise-form}))

(def token-handler (cl-ep/token-handler))

(defn authorise-client [request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user-login])
        response (auth-handler request)]
    (user/add-authorised-client-for-user! user-email client-id)
    response))

(defn authorise [request]
  (let [user-login (get-in request [:session :user-login])
        request (update-in request [:session] dissoc :csrf-token)
        access-token (get-in request [:session :access_token])
        client-id (get-in request [:params :client_id])
        redirect-uri (get-in request [:params :redirect_uri])
        response (auth-handler (assoc-in request [:headers "accept"] "text/html"))]
    (-> response
        (assoc-in [:session :client-id] client-id)
        (assoc-in [:session :user-login] user-login)
        (assoc-in [:session :redirect-uri] redirect-uri)
        (assoc-in [:session :access_token] access-token))))

(defn validate-token [request]
  (let [auth-code (get-in request [:params :code])
        user (user/retrieve-user-with-auth-code auth-code)
        user-login (:login user)
        user-id (:uid user)
        response (token-handler request)
        body (-> response
                 :body
                 (json/parse-string keyword)
                 (assoc :user-email user-login)
                 (assoc :user-id user-id)
                 (json/generate-string))]
    (-> response
        (assoc :body body)
        (assoc-in [:session :user-login] user-login))))

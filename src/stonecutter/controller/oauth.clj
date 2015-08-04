(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as cl-ep]
            [cheshire.core :as json]
            [cemerick.url :as url]
            [clojure.tools.logging :as log]
            [stonecutter.routes :as routes]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as client]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.view.authorise-failure :as authorise-failure]
            [stonecutter.helper :as sh]
            [stonecutter.db.storage :as storage]))

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
  (let [client-id (get-in request [:params :client_id])
        client (client/retrieve-client client-id)
        client-name (:name client)
        redirect-uri (get-in request [:params :redirect_uri])
        callback-uri-with-error (add-error-to-uri redirect-uri)
        request (-> request
                    (assoc-in [:context :client-name] client-name)
                    (assoc-in [:params :callback-uri-with-error] callback-uri-with-error))]

    (sh/enlive-response (authorise-failure/show-authorise-failure request) (:context request))))


(defn auto-approver [request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user-login])]
    (user/is-authorised-client-for-user? user-email client-id)))

(defn auth-handler [request]
  ((cl-ep/authorization-handler
     @storage/client-store
     @storage/token-store
     @storage/auth-code-store
     {:auto-approver                  auto-approver
      :user-session-required-redirect (routes/path :show-sign-in-form)
      :authorization-form             show-authorise-form
      }) request))

(defn authorise-client [request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user-login])
        response (auth-handler request)]
    (user/add-authorised-client-for-user! user-email client-id)
    response))

(defn is-redirect-uri-valid? [client-id redirect-uri]
  (let [client-url (:url (client/retrieve-client client-id))]
    (when (and client-url redirect-uri)
      (= (:host (url/url client-url)) (:host (url/url redirect-uri))))))

(defn authorise [request]
  (let [client-id (get-in request [:params :client_id])
        redirect-uri (get-in request [:params :redirect_uri])
        user-login (get-in request [:session :user-login])
        request (update-in request [:session] dissoc :csrf-token)
        access-token (get-in request [:session :access_token])]
    (if (is-redirect-uri-valid? client-id redirect-uri)
      (-> request
          (assoc-in [:headers "accept"] "text/html")
          auth-handler
          (assoc-in [:session :user-login] user-login)
          (assoc-in [:session :access_token] access-token))
      (do
        (log/warn "Invalid query params for authorisation request")
        {:status 403}))))

(defn token-handler [request]
  ((cl-ep/token-handler @storage/user-store
                        @storage/client-store
                        @storage/token-store
                        @storage/auth-code-store) request))

(defn validate-token [request]
  (let [auth-code (get-in request [:params :code])
        user (user/retrieve-user-with-auth-code auth-code)
        user-login (:login user)
        user-id (:uid user)
        confirmed? (:confirmed? user)
        response (token-handler request)
        body (-> response
                 :body
                 (json/parse-string keyword)
                 (assoc :user-email user-login)
                 (assoc :user-id user-id)
                 (assoc :user-email-confirmed confirmed?)
                 (json/generate-string))]
    (-> response
        (assoc :body body))))

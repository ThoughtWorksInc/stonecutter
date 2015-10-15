(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as cl-ep]
            [ring.util.response :as r]
            [cheshire.core :as json]
            [cemerick.url :as url]
            [clojure.tools.logging :as log]
            [stonecutter.config :as c]
            [stonecutter.routes :as routes]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as client]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.view.authorise-failure :as authorise-failure]
            [stonecutter.helper :as sh]
            [stonecutter.session :as session]))

(defn show-authorise-form [client-store request]
  (let [client-id (get-in request [:params :client_id])]
    (when-let [client (client/retrieve-client client-store client-id)]
      (let [context (assoc (:context request) :client client)]
        (-> (assoc request :context context)
            authorise/authorise-form
            (sh/enlive-response request))))))

(defn add-error-to-uri [uri]
  (str uri "?error=access_denied"))

(defn show-authorise-failure [client-store request]
  (let [client-id (get-in request [:params :client_id])
        client (client/retrieve-client client-store client-id)
        client-name (:name client)
        redirect-uri (get-in request [:params :redirect_uri])
        callback-uri-with-error (add-error-to-uri redirect-uri)
        request (-> request
                    (assoc-in [:context :client-name] client-name)
                    (assoc-in [:params :callback-uri-with-error] callback-uri-with-error))]

    (sh/enlive-response (authorise-failure/show-authorise-failure request) request)))

(defn auto-approver [user-store request]
  (let [client-id (get-in request [:params :client_id])
        user-email (session/request->user-login request)]
    (user/is-authorised-client-for-user? user-store user-email client-id)))

(defn auth-handler [auth-code-store client-store user-store token-store request]
  (let [configuration {:auto-approver                  (partial auto-approver user-store)
                       :user-session-required-redirect (routes/path :index)
                       :authorization-form             (partial show-authorise-form client-store)}
        configured-authorization-handler (cl-ep/authorization-handler client-store token-store
                                                                      auth-code-store configuration)]
    (configured-authorization-handler request)))

(defn authorise-client [auth-code-store client-store user-store token-store request]
  (let [client-id (get-in request [:params :client_id])
        user-email (session/request->user-login request)
        response (auth-handler auth-code-store client-store user-store token-store request)]
    (user/add-authorised-client-for-user! user-store user-email client-id)
    response))

(defn is-redirect-uri-valid? [client-store client-id redirect-uri]
  (let [client-url (:url (client/retrieve-client client-store client-id))]
    (when (and client-url redirect-uri)
      (= (:host (url/url client-url)) (:host (url/url redirect-uri))))))

(defn remove-csrf-token [request]
  (update-in request [:session] dissoc :csrf-token))

(defn add-html-accept [request]
  (assoc-in request [:headers "accept"] "text/html"))

(defn authorise [auth-code-store client-store user-store token-store request]
  (let [client-id (get-in request [:params :client_id])
        redirect-uri (get-in request [:params :redirect_uri])
        user-login (session/request->user-login request)
        clauth-request (-> request remove-csrf-token add-html-accept)
        access-token (session/request->access-token request)]
    (if (is-redirect-uri-valid? client-store client-id redirect-uri)
      (-> (auth-handler auth-code-store client-store user-store token-store clauth-request)
          (session/set-user-login user-login)
          (session/set-access-token access-token))
      (do
        (log/warn "Invalid query params for authorisation request")
        {:status 403}))))

(defn token-handler [auth-code-store client-store user-store token-store request]
  ((cl-ep/token-handler user-store
                        client-store
                        token-store
                        auth-code-store) request))

(defn generate-user-info [user]
  {:email (:login user)
   :sub (:uid user)
   :email_verified (:confirmed? user)
   :role (:role user)})

(defn assoc-in-json [json k v]
  (-> json 
      (json/parse-string keyword)
      (assoc k v)
      (json/generate-string)))

(defn token-response-body [config-m id-token-generator clauth-response-body user-info auth-code-record]
  (case (:scope auth-code-record) 
    "openid"
    (let [subject (:sub user-info)
          client-id (get-in auth-code-record [:client :client-id])
          id-token-lifetime (c/open-id-connect-id-token-lifetime-minutes config-m)
          additional-claims (dissoc user-info :sub)
          id-token (id-token-generator subject client-id id-token-lifetime additional-claims)]
      (assoc-in-json clauth-response-body :id_token id-token)) 

    ;default
    (assoc-in-json clauth-response-body :user-info user-info)))

(defn validate-token [config-m auth-code-store client-store user-store token-store id-token-generator request]
  (let [auth-code (get-in request [:params :code])
        auth-code-record (user/retrieve-auth-code auth-code-store auth-code)
        clauth-response (token-handler auth-code-store client-store user-store token-store request)]
    (if (= 200 (:status clauth-response))
      (let [user-info (generate-user-info (:subject auth-code-record))
            body (token-response-body config-m id-token-generator (:body clauth-response) user-info auth-code-record)] 
        (-> clauth-response (assoc :body body)))
      clauth-response)))

(defn jwk-set [json-web-key-set request]
  (-> (r/response json-web-key-set)
      (r/content-type "application/json")))

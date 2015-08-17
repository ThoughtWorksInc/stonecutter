(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as cl-ep]
            [cheshire.core :as json]
            [cemerick.url :as url]
            [clojure.tools.logging :as log]
            [stonecutter.config :as c]
            [stonecutter.routes :as routes]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as client]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.view.authorise-failure :as authorise-failure]
            [stonecutter.helper :as sh]))

(defn show-authorise-form [client-store request]
  (let [client-id (get-in request [:params :client_id])]
    (when-let [client (client/retrieve-client client-store client-id)]
      (let [context (assoc (:context request) :client client)]
        (-> (assoc request :context context)
            authorise/authorise-form
            (sh/enlive-response context))))))

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

    (sh/enlive-response (authorise-failure/show-authorise-failure request) (:context request))))

(defn auto-approver [user-store request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user-login])]
    (user/is-authorised-client-for-user? user-store user-email client-id)))

(defn auth-handler [auth-code-store client-store user-store token-store request]
  (let [configuration {:auto-approver                  (partial auto-approver user-store)
                       :user-session-required-redirect (routes/path :show-sign-in-form)
                       :authorization-form             (partial show-authorise-form client-store)}
        configured-authorization-handler (cl-ep/authorization-handler client-store token-store
                                                                      auth-code-store configuration)]
    (configured-authorization-handler request)))

(defn authorise-client [auth-code-store client-store user-store token-store request]
  (let [client-id (get-in request [:params :client_id])
        user-email (get-in request [:session :user-login])
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
        user-login (get-in request [:session :user-login])
        clauth-request (-> request remove-csrf-token add-html-accept)
        access-token (get-in request [:session :access_token])]
    (if (is-redirect-uri-valid? client-store client-id redirect-uri)
      (-> (auth-handler auth-code-store client-store user-store token-store clauth-request)
          (assoc-in [:session :user-login] user-login)
          (assoc-in [:session :access_token] access-token))
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

(defn clauth-response->token-response [config-m id-token-generator clauth-response auth-code-record]
  (let [user-info (generate-user-info (:subject auth-code-record))
        scope (:scope auth-code-record)]
    (case scope
      "openid"
      (let [id-token (id-token-generator (c/base-url config-m) (:sub user-info)
                                         (get-in auth-code-record [:client :client-id])
                                         (c/open-id-connect-id-token-lifetime-minutes config-m)
                                         (:email user-info))]
        (assoc clauth-response :body (json/generate-string {:id_token id-token})))

      (let [body (-> clauth-response
                     :body
                     (json/parse-string keyword)
                     (assoc :user-info user-info)
                     (json/generate-string))]
        (-> clauth-response
            (assoc :body body))))))

(defn validate-token [config-m auth-code-store client-store user-store token-store id-token-generator request]
  (let [auth-code (get-in request [:params :code])
        auth-code-record (user/retrieve-auth-code auth-code-store auth-code)
        clauth-response (token-handler auth-code-store client-store user-store token-store request)]
    (clauth-response->token-response config-m id-token-generator clauth-response auth-code-record)))

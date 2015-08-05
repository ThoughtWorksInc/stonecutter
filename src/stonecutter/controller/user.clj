(ns stonecutter.controller.user
  (:require [clauth.token :as cl-token]
            [clauth.endpoints :as cl-ep]
            [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [stonecutter.util.ring :as util-ring]
            [stonecutter.routes :as routes]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.validation :as v]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as c]
            [stonecutter.db.confirmation :as conf]
            [stonecutter.email :as email]
            [stonecutter.view.register :as register]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.delete-account :as delete-account]
            [stonecutter.view.change-password :as change-password]
            [stonecutter.view.unshare-profile-card :as unshare-profile-card]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.helper :as sh]
            [stonecutter.db.storage :as storage]
            [stonecutter.config :as config]))

(declare redirect-to-profile-created redirect-to-profile-deleted)

(defn signed-in? [request]
  (let [session (:session request)]
    (and (:user-login session) (:access_token session))))

(defn preserve-session [response request]
  (-> response
      (assoc :session (:session request))))

(defn show-registration-form [request]
  (sh/enlive-response (register/registration-form request) (:context request)))

(defn send-confirmation-email! [user email confirmation-id config-m]
  (let [app-name (config/app-name config-m)
        base-url (config/base-url config-m)]
    (email/send! :confirmation email {:confirmation-id confirmation-id
                                      :app-name app-name
                                      :base-url base-url}))
  user)

(defn register-user [user-store request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        confirmation-id (uuid/uuid)
        config-m (get-in request [:context :config-m])
        err (v/validate-registration params (partial user/is-duplicate-user? user-store))
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (do (conf/store! @storage/confirmation-store email confirmation-id)
          (-> (user/store-user! user-store email password)
              (send-confirmation-email! email confirmation-id config-m)
              (redirect-to-profile-created request)
              (assoc :flash :confirm-email-sent))) 
      (show-registration-form request-with-validation-errors))))

(defn show-change-password-form [request]
  (sh/enlive-response (change-password/change-password-form request) (:context request)))

(defn change-password [user-store request]
  (let [email (get-in request [:session :user-login])
        params (:params request)
        current-password (:current-password params)
        new-password (:new-password params)
        err (v/validate-change-password params)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (if (user/authenticate-and-retrieve-user user-store email current-password)
        (do (user/change-password! user-store email new-password)
            (-> (r/redirect (routes/path :show-profile))
                (assoc :flash :password-changed)))
        (-> request-with-validation-errors
            (assoc-in [:context :errors :current-password] :invalid)
            (show-change-password-form)))
      (show-change-password-form request-with-validation-errors))))

(defn show-sign-in-form [request]
  (if (signed-in? request)
    (-> (r/redirect (routes/path :home)) (preserve-session request))
    (sh/enlive-response (sign-in/sign-in-form request) (:context request))))

(defn generate-login-access-token [token-store user]
  (:token (cl-token/create-token token-store nil user)))

(defn sign-in [user-store token-store request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-sign-in params)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (if-let [user (user/authenticate-and-retrieve-user user-store email password)]
        (let [access-token (generate-login-access-token token-store user)]
          (-> request
              (cl-ep/return-to-handler (routes/path :show-profile))
              (assoc-in [:session :user-login] (:login user))
              (assoc-in [:session :access_token] access-token)))
        (-> request-with-validation-errors
            (assoc-in [:context :errors :credentials] :invalid)
            show-sign-in-form))
      (show-sign-in-form request-with-validation-errors))))

(defn sign-out [request]
  (-> request
      (update-in [:session] dissoc :user-login)
      cl-ep/logout-handler))

(defn show-delete-account-confirmation [request]
  (sh/enlive-response (delete-account/delete-account-confirmation request) (:context request)))

(defn delete-account [user-store request]
  (let [email (get-in request [:session :user-login])]
    (user/delete-user! user-store email)
    (redirect-to-profile-deleted)))

(defn redirect-to-profile-created [user request]
  (-> (r/redirect (routes/path :show-profile-created))
      (preserve-session request)
      (assoc-in [:session :user-login] (:login user))
      (assoc-in [:session :access_token] (generate-login-access-token @storage/token-store user))))

(defn redirect-to-profile-deleted []
  (assoc (r/redirect (routes/path :show-profile-deleted)) :session nil))

(defn show-profile [client-store user-store request]
  (let [email (get-in request [:session :user-login])
        user (user/retrieve-user user-store email)
        confirmed? (:confirmed? user)
        authorised-client-ids (:authorised-clients user)
        authorised-clients (map #(c/retrieve-client client-store %)
                                authorised-client-ids)]
    (-> request
        (assoc-in [:context :authorised-clients] authorised-clients)
        (assoc-in [:context :confirmed?] confirmed?)
        profile/profile
        (sh/enlive-response (:context request)))))

(defn get-in-session [request key]
  (get-in request [:session key]))

(defn from-app? [request]
  (get-in-session request :return-to))

(defn show-profile-created [request]
  (let [request (assoc request :params {:from-app (from-app? request)})]
    (-> (sh/enlive-response (profile-created/profile-created request) (:context request))
        (preserve-session request)
        (update-in [:session] #(dissoc % :return-to)))))

(defn show-profile-deleted [request]
  (sh/enlive-response (delete-account/profile-deleted request) (:context request)))

(defn show-unshare-profile-card [client-store user-store request]
  (if-let [client-id (get-in request [:params :client_id])]
    (if (user/is-authorised-client-for-user? user-store (get-in request [:session :user-login]) client-id)
      (let [client (c/retrieve-client client-store client-id)]
        (-> (assoc-in request [:context :client] client)
            unshare-profile-card/unshare-profile-card
            (sh/enlive-response (:context request))))
      (r/redirect (routes/path :show-profile)))
    {:status 404}))

(defn unshare-profile-card [user-store request]
  (let [email (get-in request [:session :user-login])
        client-id (get-in request [:params :client_id])]
    (user/remove-authorised-client-for-user! user-store email client-id)
    (r/redirect (routes/path :show-profile))))

(defn home [request]
  (r/redirect (routes/path :show-profile)))

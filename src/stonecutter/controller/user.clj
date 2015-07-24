(ns stonecutter.controller.user
  (:require [clauth.token :as cl-token]
            [clauth.endpoints :as cl-ep]
            [ring.util.response :as r]
            [stonecutter.routes :as routes]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.validation :as v]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as c]
            [stonecutter.view.register :as register]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.delete-account :as delete-account]
            [stonecutter.view.change-password :as change-password]
            [stonecutter.view.unshare-profile-card :as unshare-profile-card]
            [stonecutter.helper :as sh]))

(declare redirect-to-profile-created redirect-to-profile-deleted)

(defn signed-in? [request]
  (let [session (:session request)]
    (and (:user-login session) (:access_token session))))

(defn preserve-session [response request]
  (-> response
      (assoc :session (:session request))))

(defn show-registration-form [request]
  (sh/enlive-response (register/registration-form request) (:context request)))

(defn register-user [request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-registration params user/is-duplicate-user?)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (-> (user/store-user! email password)
          (redirect-to-profile-created request))
      (show-registration-form request-with-validation-errors))))

(defn show-change-password-form [request]
  (sh/enlive-response (change-password/change-password-form request) (:context request)))

(defn change-password [request]
  (let [email (get-in request [:session :user-login])
        params (:params request)
        current-password (:current-password params)
        new-password (:new-password params)
        err (v/validate-change-password params)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (if (user/authenticate-and-retrieve-user email current-password)
        (do (user/change-password! email new-password)
            (r/redirect (routes/path :show-profile)))
        (-> request-with-validation-errors
            (assoc-in [:context :errors :current-password] :invalid)
            (show-change-password-form)))
      (show-change-password-form request-with-validation-errors))))

(defn show-sign-in-form [request]
  (if (signed-in? request)
    (-> (r/redirect (routes/path :home)) (preserve-session request))
    (sh/enlive-response (sign-in/sign-in-form request) (:context request))))

(defn generate-login-access-token [user]
  (:token (cl-token/create-token nil user)))

(defn sign-in [request]
  (let [return-to (get-in request [:session :return-to])
        params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-sign-in params)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (if-let [user (user/authenticate-and-retrieve-user email password)]
        (let [access-token (generate-login-access-token user)]
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

(defn delete-account [request]
  (let [email (get-in request [:session :user-login])]
    (user/delete-user! email)
    (redirect-to-profile-deleted)))

(defn redirect-to-profile-created [user request]
  (-> (r/redirect (routes/path :show-profile-created))
      (preserve-session request)
      (assoc-in [:session :user-login] (:login user))
      (assoc-in [:session :access_token] (generate-login-access-token user))))

(defn redirect-to-profile-deleted []
  (assoc (r/redirect (routes/path :show-profile-deleted)) :session nil))

(defn show-profile [request]
  (let [email (get-in request [:session :user-login])
        user (user/retrieve-user email)
        authorised-client-ids (:authorised-clients user)
        authorised-clients (map c/retrieve-client authorised-client-ids)]
    (-> (assoc-in request [:context :authorised-clients] authorised-clients)
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

(defn show-unshare-profile-card [request]
  (if-let [client-id (get-in request [:params :client_id])]
    (if (user/is-authorised-client-for-user? (get-in request [:session :user-login]) client-id)
      (let [client (c/retrieve-client client-id)]
        (-> (assoc-in request [:context :client] client)
            unshare-profile-card/unshare-profile-card
            (sh/enlive-response (:context request))))
      (r/redirect (routes/path :show-profile)))
    {:status 404}))

(defn unshare-profile-card [request]
  (let [email (get-in request [:session :user-login])
        client-id (get-in request [:params :client_id])]
    (user/remove-authorised-client-for-user! email client-id)
    (r/redirect (routes/path :show-profile))))

(defn home [request]
  (r/redirect (routes/path :show-profile)))

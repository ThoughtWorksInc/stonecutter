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
            [stonecutter.email :as email]
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

(defn send-confirmation-email! [user email]
  (email/send! :confirmation email {:confirmation-id (:confirmation-id user)})
  user)

(defn register-user [request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-registration params user/is-duplicate-user?)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (-> (user/store-user! email password)
          (send-confirmation-email! email)
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

(defn show-confirm-sign-in-form [request]
  (sh/enlive-response (sign-in/confirmation-sign-in-form request) (:context request))
  )

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
        confirmed? (:confirmed? user)
        authorised-client-ids (:authorised-clients user)
        authorised-clients (map c/retrieve-client authorised-client-ids)]
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

(defn confirm-email [request]
  (if (signed-in? request)
    (let [user-email (get-in request [:session :user-login])
          user (user/retrieve-user user-email)]
      (log/debug (format "confirm-email Confirm-email user '%s' signed in." user-email)) 
      (if (= (get-in request [:params :confirmation-id] :no-confirmation-id-in-query) (:confirmation-id user))
        (do  
          (user/confirm-email! user)
          (r/redirect (routes/path :show-profile)))
        (-> (r/redirect (str (:uri request) "?" (:query-string request)))
            (preserve-session request)
            (update-in [:session] #(dissoc % :user-login :access_token)))))
    (do (log/debug "Confirm-email user not signed in.")
        (-> (r/redirect (routes/path :show-sign-in-form))
            (preserve-session request)
            (assoc-in [:session :return-to] (util-ring/complete-uri-of request))))))

(defn confirmation-sign-in [request]
  {}
  )

(defn confirm-email-with-id [request]
 (if (signed-in? request)
  (let [user-email (get-in request [:session :user-login])
        user (user/retrieve-user user-email)]
    (log/debug (format "confirm-email-with-id Confirm-email user '%s' signed in. BOO!" user-email)) 
    (if (= (get-in request [:params :confirmation-id] :no-confirmation-id-in-query) (:confirmation-id user))
        (do  
          (log/debug (format "confirmation-ids match. Confirming user's email."))
          (user/confirm-email! user)
          (r/redirect (routes/path :show-profile)))
        (-> (r/redirect (routes/path :confirm-email-with-id
                                     :confirmation-id (get-in request [:params :confirmation-id])))
            (preserve-session request)
            (update-in [:session] #(dissoc % :user-login :access_token)))))
  (do (log/debug "Confirm-email user not signed in.")
      (-> (r/redirect (routes/path :confirmation-sign-in-form
                                   :confirmation-id (get-in request [:params :confirmation-id])))
          (preserve-session request)))))

(defn home [request]
  (r/redirect (routes/path :show-profile)))

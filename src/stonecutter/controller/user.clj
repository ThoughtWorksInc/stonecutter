(ns stonecutter.controller.user
  (:require [clauth.token :as cl-token]
            [clauth.endpoints :as cl-ep]
            [ring.util.response :as r]
            [stonecutter.routes :as routes]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.validation :as v]
            [stonecutter.db.storage :as s]
            [stonecutter.client :as c]
            [stonecutter.view.register :as register]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.delete-account :as delete-account]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.helper :as sh]))

(declare redirect-to-profile redirect-to-profile-created redirect-to-profile-deleted)

(defn signed-in? [request]
  (let [session (:session request)]
    (and (:user session) (:access_token session))))

(defn redirect-to-authorisation [return-to user client-id]
  (if-let [client (c/retrieve-client client-id)]
    (assoc (r/redirect return-to) :session {:user user :access_token (:token (cl-token/create-token client user))})
    (throw (Exception. "Invalid client"))))

(defn register-user [request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-registration params s/is-duplicate-user?)
        request (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (-> (s/store-user! email password)
          (redirect-to-profile-created request))
      (sh/html-response (register/registration-form request)))))

(defn sign-in [request]
  (let [client-id (get-in request [:session :client-id])
        return-to (get-in request [:session :return-to])
        params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-sign-in params)
        request (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (if-let [user (s/authenticate-and-retrieve-user email password)]
        (cond (and client-id return-to) (redirect-to-authorisation return-to user client-id)
              client-id (throw (Exception. "Missing return-to value"))
              :default (redirect-to-profile user))
        (->> {:credentials :invalid}
             (assoc-in request [:context :errors])
             sign-in/sign-in-form
             sh/html-response))
      (sh/html-response (sign-in/sign-in-form request)))))

(defn sign-out [request]
  (-> request
      (update-in [:session] dissoc :user)
      cl-ep/logout-handler))

(defn show-confirm-account-confirmation [request]
  (sh/enlive-response (delete-account/delete-account-confirmation request) (:context request)))

(defn delete-account [request]
  (let [email (get-in request [:session :user :login])]
    (s/delete-user! email)
    (redirect-to-profile-deleted)))

(defn redirect-to-profile [user]
  (assoc (r/redirect (routes/path :show-profile)) :session {:user user
                                                            :access_token (:token (cl-token/create-token nil user))}))

(defn preserve-session [response request]
  (-> response
      (assoc :session (:session request))))

(defn redirect-to-profile-created [user request]
  (-> (r/redirect (routes/path :show-profile-created))
      (preserve-session request)
      (assoc-in [:session :user] user)
      (assoc-in [:session :access_token] (:token (cl-token/create-token nil user)))))

(defn redirect-to-profile-deleted []
  (assoc (r/redirect (routes/path :show-profile-deleted)) :session nil))

(defn show-registration-form [request]
  (sh/html-response (register/registration-form request)))

(defn show-sign-in-form [request]
  (if (signed-in? request)
    (-> (r/redirect (routes/path :home)) (preserve-session request))
    (sh/html-response (sign-in/sign-in-form request))))

(defn show-profile [request]
  (let [email (get-in request [:session :user :login])
        user (s/retrieve-user email)
        authorised-client-ids (:authorised-clients user)
        authorised-clients (map c/retrieve-client authorised-client-ids)]
    (-> (assoc-in request [:context :authorised-clients] authorised-clients)
        profile/profile
        sh/html-response)))

(defn get-in-session [request key]
  (get-in request [:session key]))

(defn from-app? [request]
      (and (get-in-session request :client-id)
           (get-in-session request :return-to)))

(defn show-profile-created [request]
  (let [request (assoc request :params {:from-app (from-app? request)})]
    (sh/enlive-response (profile-created/profile-created request) (:context request))))

(defn show-profile-deleted [request]
  (sh/enlive-response (delete-account/profile-deleted request) (:context request)))

(defn home [request]
  (r/redirect (routes/path :show-profile)))

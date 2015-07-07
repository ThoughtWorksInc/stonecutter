(ns stonecutter.controller.user
  (:require [clauth.client :as client]
            [clauth.token :as token]
            [ring.util.response :as r]
            [clauth.endpoints :as ep]
            [stonecutter.routes :refer [routes path]]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.validation :as v]
            [stonecutter.storage :as s]
            [stonecutter.view.register :as register]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.helper :refer :all]))

(declare redirect-to-profile redirect-to-profile-created)

(defn redirect-to-authorisation [return-to user client-id]
  (if-let [client (client/fetch-client client-id)]
    (assoc (r/redirect return-to) :session {:user user :access_token (:token (token/create-token client user))})
    (throw (Exception. "Invalid client"))))

(defn register-user [request]
  (let [params (:params request)
        email (:email params)
        password (:password params)
        err (v/validate-registration params s/is-duplicate-user?)
        request (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (do
        (let [user (s/store-user! email password)]
          (redirect-to-profile-created {:email (:login user)})))
      (html-response (register/registration-form request)))))

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
             html-response))
      (html-response (sign-in/sign-in-form request)))))

(defn sign-out [request]
  (-> request
      (update-in [:session] dissoc :user)
      ep/logout-handler))

(defn redirect-to-profile [user]
  (assoc (r/redirect (path :show-profile)) :session {:user user
                                                     :access_token (:token (token/create-token nil user))}))

(defn redirect-to-profile-created [user]
  (assoc (r/redirect (path :show-profile-created)) :session {:user user
                                                             :access_token (:token (token/create-token nil user))}))

(defn show-registration-form [request]
  (html-response (register/registration-form request)))

(defn signed-in? [request]
  (let [session (:session request)]
    (and (:user session) (:access_token session))))

(defn preserve-session [response request]
  (-> response
      (assoc :session (:session request))))

(defn show-sign-in-form [request]
  (if (signed-in? request)
    (-> (r/redirect (path :home)) (preserve-session request))
    (html-response (sign-in/sign-in-form request))))

(defn show-profile [request]
  (if (signed-in? request)
    (html-response (profile/profile request))
    (r/redirect (path :sign-in))))

(defn show-profile-created [request]
  (html-response (profile-created/profile-created request)))

(defn show-authorise-form [request]
  (html-response (authorise/authorise-form request)))

(defn home [request]
  (if (signed-in? request)
    (r/redirect (path :show-profile))
    (r/redirect (path :sign-in))))

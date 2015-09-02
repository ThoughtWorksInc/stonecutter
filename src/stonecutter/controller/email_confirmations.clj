(ns stonecutter.controller.email-confirmations
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [stonecutter.db.confirmation :as conf]
            [stonecutter.db.user :as user]
            [stonecutter.helper :as sh]
            [stonecutter.routes :as routes]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.util.ring :as ring-util]
            [stonecutter.controller.common :as common]
            [ring.util.response :as response]))

(defn show-confirm-sign-in-form [request]
  (sh/enlive-response (sign-in/confirmation-sign-in-form request) (:context request)))

(defn confirm-users-email! [user-store confirmation-store user confirmation-id]
  (log/debug (format "confirmation-ids match. Confirming user's email."))
  (user/confirm-email! user-store user)
  (conf/revoke! confirmation-store confirmation-id)
  (-> (r/redirect (routes/path :show-profile))
      (assoc :flash :email-confirmed)))

(defn mismatch-confirmation-id-response [request]
  (log/debug (format "confirmation-ids DID NOT match. SIGNING OUT"))
  (-> (r/redirect (routes/path :confirm-email-with-id
                               :confirmation-id (get-in request [:params :confirmation-id])))
      (ring-util/preserve-session request)
      (update-in [:session] #(dissoc % :user-login :access_token))))

(defn redirect-to-confirmation-sign-in-form [request]
  (do (log/debug "Confirm-email user not signed in.")
      (-> (r/redirect (routes/path :confirmation-sign-in-form
                                   :confirmation-id (get-in request [:params :confirmation-id])))
          (ring-util/preserve-session request))))

(defn confirmation-id-exists? [confirmation-store confirmation-id]
  (not (= nil (conf/fetch confirmation-store confirmation-id))))

(defn confirm-email-with-id [user-store confirmation-store request]
  (when (confirmation-id-exists? confirmation-store (get-in request [:params :confirmation-id]))
    (if (common/signed-in? request)
      (let [user-email (get-in request [:session :user-login])
            user (user/retrieve-user user-store user-email)
            confirmation (conf/fetch confirmation-store (get-in request [:params :confirmation-id]))]
        (log/debug (format "confirm-email-with-id Confirm-email user '%s' signed in." user-email))
        (if (= (:login confirmation) (:login user))
          (confirm-users-email! user-store confirmation-store user (:confirmation-id confirmation))
          (mismatch-confirmation-id-response request)))
      (redirect-to-confirmation-sign-in-form request))))

(defn confirmation-sign-in [user-store token-store confirmation-store request]
  (let [confirmation-id (get-in request [:params :confirmation-id])
        password (get-in request [:params :password])
        email (:login (conf/fetch confirmation-store confirmation-id))
        confirm-email-path (routes/path :confirm-email-with-id :confirmation-id confirmation-id)]
    (if-let [user (user/authenticate-and-retrieve-user user-store email password)]
      (-> (response/redirect confirm-email-path)
          (common/sign-in-user token-store user))
      (-> request
          (assoc-in [:context :errors :credentials] :confirmation-invalid)
          show-confirm-sign-in-form))))

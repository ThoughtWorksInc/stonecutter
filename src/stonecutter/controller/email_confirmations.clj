(ns stonecutter.controller.email-confirmations
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [stonecutter.controller.user :as u]
            [stonecutter.db.confirmation :as conf]
            [stonecutter.db.user :as user]
            [stonecutter.routes :as routes]
            [stonecutter.view.sign-in :as sign-in]
            [stonecutter.helper :as sh]
            [stonecutter.db.storage :as storage]))

(defn show-confirm-sign-in-form [request]
  (sh/enlive-response (sign-in/confirmation-sign-in-form request) (:context request)))

(defn confirm-email-with-id [request]
 (if (u/signed-in? request)
  (let [user-email (get-in request [:session :user-login])
        user (user/retrieve-user @storage/user-store user-email)
        confirmation (conf/fetch (get-in request [:params :confirmation-id]))]
    (log/debug (format "confirm-email-with-id Confirm-email user '%s' signed in." user-email)) 
    (if (= (:login confirmation) (:login user))
        (do  
          (log/debug (format "confirmation-ids match. Confirming user's email."))
          (user/confirm-email! @storage/user-store user)
          (conf/revoke! (:confirmation-id confirmation))
          (r/redirect (routes/path :show-profile)))
        (do 
          (log/debug (format "confirmation-ids DID NOT match. SIGNING OUT"))
          (-> (r/redirect (routes/path :confirm-email-with-id
                                       :confirmation-id (get-in request [:params :confirmation-id])))
              (u/preserve-session request)
              (update-in [:session] #(dissoc % :user-login :access_token))))))
  (do (log/debug "Confirm-email user not signed in.")
      (-> (r/redirect (routes/path :confirmation-sign-in-form
                                   :confirmation-id (get-in request [:params :confirmation-id])))
          (u/preserve-session request)))))

(defn confirmation-sign-in [request]
  (let [confirmation-id (get-in request [:params :confirmation-id])
        password (get-in request [:params :password])
        email (:login (conf/fetch confirmation-id))]
    (if-let [user (user/authenticate-and-retrieve-user @storage/user-store email password)]
        (let [access-token (u/generate-login-access-token user)]
          (-> (r/redirect (routes/path :confirm-email-with-id
                                       :confirmation-id confirmation-id))
              (assoc-in [:session :user-login] (:login user))
              (assoc-in [:session :access_token] access-token)))
        (-> request
            (assoc-in [:context :errors :credentials] :confirmation-invalid)
            show-confirm-sign-in-form))))

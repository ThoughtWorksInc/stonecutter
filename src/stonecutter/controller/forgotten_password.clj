(ns stonecutter.controller.forgotten-password
  (:require [clauth.store :as cl-store]
            [clojure.string :as string]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [stonecutter.validation :as v]
            [stonecutter.view.forgotten-password :as forgotten-password-view]
            [stonecutter.view.forgotten-password-confirmation :as forgotten-password-confirmation-view]
            [stonecutter.view.reset-password :as reset-password]
            [stonecutter.db.forgotten-password :as db]
            [stonecutter.helper :as sh]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.db.user :as user]
            [stonecutter.config :as config]
            [stonecutter.routes :as r]
            [stonecutter.db.token :as token]))

(defn request->forgotten-password-id [request]
  (get-in request [:params :forgotten-password-id]))

(defn request->new-password [request]
  (get-in request [:params :new-password]))

(defn show-forgotten-password-form [request]
  (sh/enlive-response (forgotten-password-view/forgotten-password-form request) (:context request)))

(defn forgotten-password-form-post [email-sender user-store forgotten-password-store request]
  (let [config-m (get-in request [:context :config-m])
        params (:params request)
        email-address (string/lower-case (:email params))
        err (v/validate-forgotten-password params)
        app-name (config/app-name config-m)
        base-url (config/base-url config-m)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (do
        (if (user/retrieve-user user-store email-address)
          (let [forgotten-password-id (uuid/uuid)]
            (db/store-id-for-user! forgotten-password-store forgotten-password-id email-address)
            (email/send! email-sender :forgotten-password email-address {:app-name app-name :base-url base-url :forgotten-password-id forgotten-password-id}))
          (log/warn (format "User %s does not exist so reset password e-mail not sent." email-address)))
        (response/redirect (routes/path :show-forgotten-password-confirmation)))
      (show-forgotten-password-form request-with-validation-errors))))

(defn show-forgotten-password-confirmation [request]
  (sh/enlive-response (forgotten-password-confirmation-view/forgotten-password-confirmation request) (:context request)))

(defn show-reset-password-form [forgotten-password-store user-store request]
  (let [forgotten-password-id (request->forgotten-password-id request)]
    (when-let [forgotten-password-record (cl-store/fetch forgotten-password-store forgotten-password-id)]
      (when (user/retrieve-user user-store (:login forgotten-password-record))
        (sh/enlive-response (reset-password/reset-password-form request) (:context request))))))

(defn reset-password-form-post [forgotten-password-store user-store token-store request]
  (let [params (:params request)
        err (v/validate-reset-password params)
        request-with-validation-errors (assoc-in request [:context :errors] err)
        forgotten-password-id (request->forgotten-password-id request)
        new-password (request->new-password request)]
    (when-let [forgotten-password-record (cl-store/fetch forgotten-password-store forgotten-password-id)]
      (let [email-address (:login forgotten-password-record)]
        (when (user/retrieve-user user-store email-address)
          (if (empty? err)
            (let [updated-user (user/change-password! user-store email-address new-password)]
              (cl-store/revoke! forgotten-password-store forgotten-password-id)
                (-> (response/redirect (r/path :home))
                    (assoc-in [:session :user-login] email-address) ;; TODO JOHN 11/08/2015 pull this session assoc into utility function
                    (assoc-in [:session :access_token] (token/generate-login-access-token token-store updated-user))))
            (show-reset-password-form forgotten-password-store user-store request-with-validation-errors)))))))

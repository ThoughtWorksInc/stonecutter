(ns stonecutter.controller.forgotten-password
  (:require [clauth.store :as cl-store]
            [clojure.string :as string]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [stonecutter.validation :as v]
            [stonecutter.view.forgotten-password :as forgotten-password-view]
            [stonecutter.view.forgotten-password-confirmation :as forgotten-password-confirmation-view]
            [stonecutter.view.reset-password :as reset-password]
            [stonecutter.helper :as sh]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.db.user :as user]
            [stonecutter.config :as config]))

(defn request->forgotten-password-id [request]
  (get-in request [:params :forgotten-password-id]))

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
            (cl-store/store! forgotten-password-store :forgotten-password-id {:forgotten-password-id forgotten-password-id :login email-address})
            (email/send! email-sender :forgotten-password email-address {:app-name app-name :base-url base-url :forgotten-password-id forgotten-password-id}))
          (log/warn (format "User %s does not exist so reset password e-mail not sent." email-address)))
        (response/redirect (routes/path :show-forgotten-password-confirmation)))
      (show-forgotten-password-form request-with-validation-errors))))

(defn show-forgotten-password-confirmation [request]
  (sh/enlive-response (forgotten-password-confirmation-view/forgotten-password-confirmation request) (:context request)))

(defn show-reset-password-form [forgotten-password-store user-store request]
  (let [forgotten-password-id (request->forgotten-password-id request)]
    (when-let [forgotten-password-record (cl-store/fetch forgotten-password-store forgotten-password-id)]
      (when-let [user (user/retrieve-user user-store (:login forgotten-password-record))]
        (sh/enlive-response (reset-password/reset-password-form request) (:context request))))))

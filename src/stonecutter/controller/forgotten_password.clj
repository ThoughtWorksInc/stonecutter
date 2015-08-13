(ns stonecutter.controller.forgotten-password
  (:require [clauth.store :as cl-store]
            [clojure.string :as string]
            [ring.util.response :as response]
            [clojure.tools.logging :as log]
            [stonecutter.validation :as v]
            [stonecutter.view.forgotten-password :as forgotten-password-view]
            [stonecutter.view.forgotten-password-confirmation :as forgotten-password-confirmation-view]
            [stonecutter.view.reset-password :as reset-password]
            [stonecutter.controller.common :as common]
            [stonecutter.db.forgotten-password :as db]
            [stonecutter.helper :as sh]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.db.user :as user]
            [stonecutter.config :as config]
            [stonecutter.db.expiry :as e]))

(defn request->forgotten-password-id [request]
  (get-in request [:params :forgotten-password-id]))

(defn request->new-password [request]
  (get-in request [:params :new-password]))

(defn show-forgotten-password-form [request]
  (sh/enlive-response (forgotten-password-view/forgotten-password-form request) (:context request)))

(defn create-or-retrieve-id [forgotten-password-store clock email-address expiry-hours]
  (if-let [existing-id (:forgotten-password-id (db/forgotten-password-doc-by-login forgotten-password-store clock email-address))]
    existing-id
    (let [forgotten-password-id (uuid/uuid)]
      (db/store-id-for-user! forgotten-password-store clock forgotten-password-id email-address expiry-hours)
      forgotten-password-id)))

(defn forgotten-password-form-post [email-sender user-store forgotten-password-store clock request]
  (let [config-m (get-in request [:context :config-m])
        params (:params request)
        email-address (string/lower-case (:email params))
        err (v/validate-forgotten-password params (partial user/user-exists? user-store))
        app-name (config/app-name config-m)
        base-url (config/base-url config-m)
        request-with-validation-errors (assoc-in request [:context :errors] err)
        email-expiry (config/password-reset-expiry config-m)]
    (if (empty? err)
      (let [forgotten-password-id (create-or-retrieve-id forgotten-password-store clock email-address email-expiry)]
        (email/send! email-sender :forgotten-password email-address {:app-name app-name :base-url base-url :forgotten-password-id forgotten-password-id})
        (response/redirect (routes/path :show-forgotten-password-confirmation)))
      (show-forgotten-password-form request-with-validation-errors))))

(defn show-forgotten-password-confirmation [request]
  (sh/enlive-response (forgotten-password-confirmation-view/forgotten-password-confirmation request) (:context request)))


(defn redirect-to-forgotten-password-form []
  (-> (response/redirect (routes/path :show-forgotten-password-form))
      (assoc :flash :expired-password-reset)))

(defn show-reset-password-form [forgotten-password-store user-store clock request]
  (let [forgotten-password-id (request->forgotten-password-id request)]
    (let [forgotten-password-record (e/fetch-with-expiry forgotten-password-store clock forgotten-password-id)
          user (user/retrieve-user user-store (:login forgotten-password-record))]
      (if (and forgotten-password-record user)
        (sh/enlive-response (reset-password/reset-password-form request) (:context request))
        (redirect-to-forgotten-password-form)))))

(defn reset-password-form-post [forgotten-password-store user-store token-store clock request]
  (let [params (:params request)
        err (v/validate-reset-password params)
        request-with-validation-errors (assoc-in request [:context :errors] err)
        forgotten-password-id (request->forgotten-password-id request)
        new-password (request->new-password request)]
    (if-let [forgotten-password-record (e/fetch-with-expiry forgotten-password-store clock forgotten-password-id)]
      (let [email-address (:login forgotten-password-record)]
        (if (user/retrieve-user user-store email-address)
          (if (empty? err)
            (let [updated-user (user/change-password! user-store email-address new-password)]
              (cl-store/revoke! forgotten-password-store forgotten-password-id)
              (-> (common/sign-in-to-home token-store updated-user)
                  (assoc :flash :password-changed)))
            (show-reset-password-form forgotten-password-store user-store clock request-with-validation-errors))
          (redirect-to-forgotten-password-form)))
      (redirect-to-forgotten-password-form))))

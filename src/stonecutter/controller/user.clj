(ns stonecutter.controller.user
  (:require [clauth.endpoints :as cl-ep]
            [ring.util.response :as r]
            [stonecutter.routes :as routes]
            [stonecutter.validation :as v]
            [stonecutter.db.user :as user]
            [stonecutter.db.client :as c]
            [stonecutter.email :as email]
            [stonecutter.view.index :as index]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.delete-account :as delete-account]
            [stonecutter.view.change-password :as change-password]
            [stonecutter.view.unshare-profile-card :as unshare-profile-card]
            [stonecutter.view.change-email :as change-email]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.helper :as sh]
            [stonecutter.config :as config]
            [stonecutter.util.ring :as ring-util]
            [stonecutter.controller.common :as common]
            [ring.util.response :as response]
            [stonecutter.db.confirmation :as confirmation]
            [stonecutter.session :as session]
            [stonecutter.db.invitations :as invitations]))

(defn has-valid-invite-id? [request invitation-store]
  (let [invite-id (get-in request [:params :invite-id])]
    (invitations/fetch-by-id invitation-store invite-id)))

(defn index [request]
  (if (common/signed-in? request)
    (r/redirect (routes/path :show-profile))
    (sh/enlive-response (index/index request) request)))

(defn accept-invite [invitation-store request]
  (if-let [invite-map (has-valid-invite-id? request invitation-store)]
    (let [request-with-email (assoc-in request [:params :registration-email] (:email invite-map))]
      (sh/enlive-response (index/accept-invite request-with-email) request-with-email))
    (sh/enlive-response (index/index request) request)))

(defn send-confirmation-email! [email-sender email confirmation-id config-m]
  (let [app-name (config/app-name config-m)
        base-url (config/base-url config-m)]
    (email/send! email-sender :confirmation email {:confirmation-id confirmation-id
                                                   :app-name        app-name
                                                   :base-url        base-url})))

(defn send-confirmation-email-with-new-id! [confirmation-store email-sender email config-m]
  (let [confirmation-id (uuid/uuid)]
    (confirmation/store! confirmation-store email confirmation-id)
    (send-confirmation-email! email-sender email confirmation-id config-m)))

(defn register-user [user-store token-store confirmation-store email-sender request]
  (let [params (:params request)
        first-name (:registration-first-name params)
        last-name (:registration-last-name params)
        email (:registration-email params)
        password (:registration-password params)
        config-m (get-in request [:context :config-m])]
    (if (empty? (get-in request [:context :errors]))
      (let [user (user/store-user! user-store first-name last-name email password)]
        (send-confirmation-email-with-new-id! confirmation-store email-sender email config-m)
        (-> (response/redirect (routes/path :show-profile-created))
            (common/sign-in-user token-store user (:session request))
            (assoc :flash {:flash-type    :confirm-email-sent
                           :email-address (:login user)})))
      (index request))))

(defn request-with-validation-errors [user-store request]
  (let [params (:params request)
        err (v/validate-registration params (partial user/user-exists? user-store))]
    (assoc-in request [:context :errors] err)))

(defn show-change-password-form [request]
  (sh/enlive-response (change-password/change-password-form request) request))

(defn change-password [user-store email-sender request]
  (let [email (session/request->user-login request)
        params (:params request)
        new-password (:new-password params)
        err (v/validate-change-password params (partial user/authenticate-and-retrieve-user user-store email))
        request-with-validation-errors (assoc-in request [:context :errors] err)
        config-m (get-in request [:context :config-m])]
    (if (empty? err)
      (do (user/change-password! user-store email new-password) ;; FIXME CW & JC | 2015/9/4 pass user authentication function into validation?
          (email/send! email-sender :change-password email {:admin-email (config/admin-login config-m) :app-name (config/app-name config-m)})
          (-> (r/redirect (routes/path :show-profile))
              (assoc :flash :password-changed)))
      (show-change-password-form request-with-validation-errors))))

(defn show-change-email-form [request]
  (sh/enlive-response (change-email/change-email-form request) request))

(defn update-user-email [user-store confirmation-store email-sender request]
  (let [email (session/request->user-login request)
        params (:params request)
        new-email (:email-address params)
        err (v/validate-email-change new-email (partial user/user-exists? user-store))
        request-with-validation-errors (assoc-in request [:context :errors] err)
        config-m (get-in request [:context :config-m])]
    (if (empty? err)
      (do (user/update-user-email! user-store email new-email)
          (send-confirmation-email-with-new-id! confirmation-store email-sender  new-email config-m)
          (-> (r/redirect (routes/path :show-profile))
              (assoc :flash :email-changed)
              (assoc :session (:session request-with-validation-errors))
              (assoc-in [:session :user-login] new-email)))
      (show-change-email-form request-with-validation-errors))))

(defn sign-in [user-store token-store request]
  (let [params (:params request)
        email (:sign-in-email params)
        password (:sign-in-password params)
        err (v/validate-sign-in params)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (if (empty? err)
      (if-let [user (user/authenticate-and-retrieve-user user-store email password)]
        (-> request
            (cl-ep/return-to-handler (routes/path :show-profile))
            (common/sign-in-user token-store user))
        (-> request-with-validation-errors
            (assoc-in [:context :errors :sign-in-credentials] :invalid)
            index))
      (index request-with-validation-errors))))

(defn sign-in-or-register [user-store token-store confirmation-store email-sender request]
  (let [form-action-type (get-in request [:params :action])]
    (case form-action-type
      "register" (register-user user-store token-store confirmation-store email-sender (request-with-validation-errors user-store request))
      "sign-in" (sign-in user-store token-store request)
      nil)))

(defn sign-out [request]
  (-> request
      (update-in [:session] dissoc :user-login)
      cl-ep/logout-handler))

(defn show-delete-account-confirmation [request]
  (sh/enlive-response (delete-account/delete-account-confirmation request) request))

(defn redirect-to-profile-deleted []
  (-> (routes/path :show-profile-deleted)
      r/redirect
      (session/replace-session-with nil)))

(defn delete-account [user-store confirmation-store request]
  (let [email (session/request->user-login request)]
    (when-let [confirmation (confirmation/retrieve-by-user-email confirmation-store email)]
      (confirmation/revoke! confirmation-store (:confirmation-id confirmation)))
    (user/delete-user! user-store email)
    (redirect-to-profile-deleted)))

(defn get-profile-picture [profile-picture-store uid config-m]
  (if-let [profile-picture (user/retrieve-profile-picture profile-picture-store uid config-m)]
    profile-picture
    config/default-profile-picture))

(defn show-profile [client-store user-store profile-picture-store request]
  (let [email (session/request->user-login request)
        user (user/retrieve-user user-store email)
        confirmed? (:confirmed? user)
        role (:role user)
        authorised-client-ids (:authorised-clients user)
        authorised-clients (map #(c/retrieve-client client-store %)
                                authorised-client-ids)
        config-m (get-in request [:context :config-m])
        profile-picture (get-profile-picture profile-picture-store (:uid user) config-m)]
    (-> request
        (assoc-in [:context :authorised-clients] authorised-clients)
        (assoc-in [:context :confirmed?] confirmed?)
        (assoc-in [:context :user-login] (:login user))
        (assoc-in [:context :user-first-name] (:first-name user))
        (assoc-in [:context :user-last-name] (:last-name user))
        (assoc-in [:context :user-profile-picture] profile-picture)
        profile/profile
        (sh/enlive-response request))))

(defn from-app? [request]
  (session/request->return-to request))

(defn update-profile-image [user-store profile-picture-store request]
  (let [email (get-in request [:session :user-login])
        user (user/retrieve-user user-store email)]
    (user/update-profile-picture! request profile-picture-store (:uid user)))
  (r/redirect (routes/path :show-profile)))

(defn show-profile-created [request]
  (let [request (assoc request :params {:from-app (from-app? request)})]
    (-> (sh/enlive-response (profile-created/profile-created request) request)
        (ring-util/preserve-session request)
        (update-in [:session] #(dissoc % :return-to)))))

(defn show-profile-deleted [request]
  (sh/enlive-response (delete-account/profile-deleted request) request))

(defn show-unshare-profile-card [client-store user-store request]
  (when-let [client-id (get-in request [:params :client_id])]
    (if (user/is-authorised-client-for-user? user-store (session/request->user-login request) client-id)
      (let [client (c/retrieve-client client-store client-id)]
        (-> (assoc-in request [:context :client] client)
            unshare-profile-card/unshare-profile-card
            (sh/enlive-response request)))
      (r/redirect (routes/path :show-profile)))))

(defn unshare-profile-card [user-store request]
  (let [email (session/request->user-login request)
        client-id (get-in request [:params :client_id])]
    (user/remove-authorised-client-for-user! user-store email client-id)
    (r/redirect (routes/path :show-profile))))

(defn resend-confirmation-email [user-store confirmation-store email-sender request]
  (let [config-m (get-in request [:context :config-m])
        email (session/request->user-login request)
        user (user/retrieve-user user-store email)]
    (if (:confirmed? user)
      (-> (r/redirect (routes/path :show-profile)) (assoc :flash :email-already-confirmed))
      (let [confirmation (confirmation/retrieve-by-user-email confirmation-store email)]
        (send-confirmation-email! email-sender email (:confirmation-id confirmation) config-m)
        (-> (r/redirect (routes/path :show-profile)) (assoc :flash :confirmation-email-sent))))))

(defn register-using-invitation [user-store token-store confirmation-store email-sender invitation-store request]
  (let [request-with-errors (request-with-validation-errors user-store request)
        errors (get-in request-with-errors [:context :errors])
        response (register-user user-store token-store confirmation-store email-sender request-with-errors)]
    (when (empty? errors)
      (invitations/remove-invite! invitation-store (get-in request [:params :invite-id]))
      (user/update-user-role! user-store (get-in request-with-errors [:params :registration-email]) (:trusted config/roles))
      response)))
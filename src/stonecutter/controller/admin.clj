(ns stonecutter.controller.admin
  (:require [ring.util.response :as r]
            [stonecutter.helper :as sh]
            [stonecutter.view.user-list :as user-list]
            [stonecutter.view.apps-list :as apps-list]
            [stonecutter.db.user :as u]
            [stonecutter.db.client :as c]
            [stonecutter.routes :as routes]
            [stonecutter.config :as config]
            [stonecutter.view.delete-app :as delete-app]
            [clojure.string :as s]
            [stonecutter.session :as session]
            [stonecutter.view.invite-user :as invite-user]
            [stonecutter.email :as email]
            [stonecutter.db.invitations :as i]
            [stonecutter.validation :as validation]))

(defn show-user-list [user-store request]
  (let [users (u/retrieve-users user-store)]
    (sh/enlive-response (-> request
                            (assoc-in [:context :users] users)
                            (user-list/user-list))
                        request)))

(defn set-user-trustworthiness [user-store request]
  (let [email (get-in request [:params :login])
        trusted? (get-in request [:params :trust-toggle])
        role (if trusted?
               (:trusted config/roles)
               (:untrusted config/roles))
        flash-key (if trusted?
                    :user-trusted
                    :user-untrusted)]

    (u/update-user-role! user-store email role)
    (-> (r/redirect (routes/path :show-user-list))
        (assoc-in [:flash :translation-key] flash-key)
        (assoc-in [:flash :updated-account-email] email))))

(defn show-apps-list [client-store request]
  (let [clients (c/retrieve-clients client-store)]
    (sh/enlive-response (-> request
                            (assoc-in [:context :clients] clients)
                            (apps-list/apps-list))
                        request)))

(defn show-invite-user-form [request]
  (sh/enlive-response (invite-user/invite-user request)
                      request))

(defn send-invite-email! [email-sender email invitation-store clock config-m]
  (let [app-name (config/app-name config-m)
        base-url (config/base-url config-m)
        invite-expiry (config/invite-expiry config-m)
        invite-id (i/generate-invite-id! invitation-store email clock invite-expiry)]
    (email/send! email-sender :invite email {:invite-id invite-id
                                             :app-name  app-name
                                             :base-url  base-url})))

(defn validate-invite-email [user-store invitation-store email-address]
  (let [already-invited? (i/fetch-by-email invitation-store email-address)
        already-registered? (u/retrieve-user user-store email-address)
        invalid? (not (validation/is-email-valid? email-address))]
    (cond-> {}
            (s/blank? email-address) (assoc :invitation-email :blank)
            already-invited? (assoc :invitation-email :invited)
            already-registered? (assoc :invitation-email :duplicate)
            invalid? (assoc :invitation-email :invalid))))

(defn send-user-invite [email-sender user-store invitation-store clock request]
  (let [email-address (get-in request [:params :email])
        err (validate-invite-email user-store invitation-store email-address)]
    (if (empty? err)
      (do (send-invite-email! email-sender email-address invitation-store clock (get-in request [:context :config-m]))
          (-> (r/redirect (routes/path :show-invite))
              (assoc-in [:flash :email-address] email-address)))
      (show-invite-user-form (assoc-in request [:context :errors] err)))))

(defn delete-app [client-store request]
  (let [app-id (get-in request [:params :app-id])
        client (c/retrieve-client client-store app-id)]
    (c/delete-client! client-store app-id)
    (-> (r/redirect (routes/path :show-apps-list))
        (assoc-in [:flash :deleted-app-name] (:name client)))))

(defn validate-client-create-form [request]
  (let [params (:params request)
        client-name (:name params)
        client-url (:url params)]
    (cond-> {}
            (s/blank? client-name) (assoc :app-name :blank)
            (s/blank? client-url) (assoc :app-url :blank))))

(defn create-client [client-store request]
  (let [client-name (get-in request [:params :name])
        client-url (get-in request [:params :url])
        err (validate-client-create-form request)]
    (if (empty? err)
      (do (c/store-client client-store client-name client-url)
          (-> (r/redirect (routes/path :show-apps-list))
              (assoc-in [:flash :added-app-name] client-name)))
      (show-apps-list client-store (assoc-in request [:context :errors] err)))))

(defn show-delete-app-form [request]
  (sh/enlive-response (delete-app/delete-app-confirmation request)
                      request))

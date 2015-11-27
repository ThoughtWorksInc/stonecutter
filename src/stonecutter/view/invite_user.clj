(ns stonecutter.view.invite-user
  (:require [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]
            [net.cgrand.enlive-html :as html]))

(def email-errors
  {:invalid   "content:admin-invite-user/invite-email-address-invalid-validation-message"
   :duplicate "content:admin-invite-user/invite-email-address-duplicate-validation-message"
   :invited   "content:admin-invite-user/invite-email-address-invited-validation-message"
   :blank     "content:admin-invite-user/invite-email-address-blank-validation-message"})

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :send-invite))))

(defn set-flash-message [enlive-m request]
  (let [email-address (get-in request [:flash :email-address])]
    (if email-address
      (html/at enlive-m [:.clj--invited-email] (html/content email-address))
      (vh/remove-element enlive-m [:.func--flash-message-container]))))

(defn add-email-errors [enlive-m err]
  (if-let [invitation-email-error (:invitation-email err)]
    (let [error-translation (get email-errors invitation-email-error)]
      (-> enlive-m
          (vh/add-error-class [:.clj--invite-user-email])
          (html/at [:.clj--invite-user-email__validation] (html/set-attr :data-l8n (or error-translation "content:admin-invite-user/invite-unknown-error")))))
    enlive-m))

(defn set-invite-email-input [enlive-m params]
  (html/at enlive-m
           [:.clj--email__input] (html/set-attr :value (:email params))))

(defn invite-user [request]
  (let [error-m (get-in request [:context :errors])]
    (-> (vh/load-template-with-lang "public/admin-invite-user.html" request)
        vh/remove-work-in-progress
        vh/set-admin-links
        set-form-action
        (set-invite-email-input (:params request))
        (add-email-errors error-m)
        (set-flash-message request)
        vh/add-anti-forgery)))

(ns stonecutter.integration.kerodon.kerodon-selectors
  (:require [net.cgrand.enlive-html :as html]))

(def registration-email-input :.func--email__input)
(def registration-password-input :.func--password__input)
(def registration-confirm-input :.func--confirm-password__input)
(def registration-submit :.func--create-profile__button)
(def registration-page-body :.func--register-page)
(def registration-email-validation-element :.clj--registration-email__validation)

(def profile-created-page-body :.func--profile-created-page)
(def profile-page-body :.func--profile-page)
(def profile-deleted-page-body :.func--profile-deleted-page)
(def profile-authorised-client-list :.func--app__list)
(def profile-authorised-client-unshare-link :.func--app-item__unshare-link)
(def profile-change-password-link :.func--change-password__link)
(def profile-delete-account-link :.func--delete-account__link)
(def profile-flash-message :.func--flash-message-container)

(def unshare-profile-card-confirm-button :.func--unshare-profile-card__button)

(def sign-in-email-input :.func--email__input)
(def sign-in-password-input :.func--password__input)
(def sign-in-submit :.func--sign-in__button)
(def sign-in-page-body :.func--sign-in-page)
(def sign-in-app-name :.func--register-now-app-name)

(def sign-out-link :.func--sign-out__link)

(def authorise-share-profile-button :.func--authorise-share-profile__button)
(def authorise-cancel-link :.func--authorise-cancel__link)

(def delete-account-button :.func--delete-account__button)
(def delete-account-page-body :.func--delete-account-page)

(def change-password-current-password-input :.func--current-password__input)
(def change-password-new-password-input :.func--new-password__input)
(def change-password-confirm-new-password-input :.func--confirm-new-password__input)
(def change-password-submit :.func--change-password__button)
(def change-password-page-body :.func--change-password-page)

(def forgotten-password-button :.func--forgot-password__button)
(def forgotten-password-email :.func--email__input)
(def forgotten-password-submit :.func--send-forgotten-password-email__button)
(def forgotten-password-email-sent-page-body :.func--forgotten-password-confirmation-page)

(def reset-password-field :.func--new-password__input)
(def reset-confirm-password-field :.func--confirm-new-password__input)
(def reset-password-submit :.func--reset-password__button)

(def error-404-page-body :.func--error-404-page)
(def error-500-page-body :.func--error-500-page)

(def css-link [:link (html/attr= :type "text/css")])

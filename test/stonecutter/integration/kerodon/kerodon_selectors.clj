(ns stonecutter.integration.kerodon.kerodon-selectors
  (:require [net.cgrand.enlive-html :as html]))

(def index-page-body :.func--index-page)
(def index-app-name :.func--welcome-app-name)

(def registration-first-name-input :.func--registration-first-name__input)
(def registration-last-name-input :.func--registration-last-name__input)
(def registration-email-input :.func--registration-email__input)
(def registration-password-input :.func--registration-password__input)
(def registration-submit :.func--create-profile__button)
(def registration-email-validation-element :.clj--registration-email__validation)

(def profile-created-page-body :.func--profile-created-page)
(def profile-created-flash :.func--flash-message-container)
(def profile-page-body :.func--profile-page)
(def profile-page-profile-card-email :.func--card-email)
(def profile-page-profile-card-name :.func--card-name)
(def profile-deleted-page-body :.func--profile-deleted-page)
(def profile-authorised-client-list :.func--app__list)
(def profile-authorised-client-unshare-link :.func--app-item__unshare-link)
(def profile-change-password-link :.func--change-password__link)
(def profile-delete-account-link :.func--delete-account__link)
(def profile-flash-message :.func--flash-message-container)
(def profile-unconfirmed-email-message :.func--unconfirmed-email-message-container)
(def profile-resend-confirmation-email :.func--resend-confirmation-email__button)

(def unshare-profile-card-confirm-button :.func--unshare-profile-card__button)

(def sign-in-email-input :.func--sign-in-email__input)
(def sign-in-password-input :.func--sign-in-password__input)
(def sign-in-submit :.func--sign-in__button)

(def confirmation-sign-in-password-input :.func--password__input)

(def sign-out-link :.func--sign-out__link)

(def user-trustworthiness-submit :.func--user-item__trust-submit)
(def user-trustworthiness-flash-message :.func--flash-message-container)

(def create-app-form-name :.func--admin-add-app-form-name)
(def create-app-form-url :.func--admin-add-app-form-url)
(def create-app-form-submit :.func--create-add__button)
(def create-app-form-flash-message :.func--flash-message-container)
(def create-app-form-flash-message-name :.func--new-app-name)

(def apps-list-item-title :.func--admin-app-item__title)
(def apps-list-item-url :.func--client-url)

(def authorise-share-profile-button :.func--authorise-share-profile__button)
(def authorise-cancel-link :.func--authorise-cancel__link)

(def delete-account-button :.func--delete-account__button)
(def delete-account-page-body :.func--delete-account-page)

(def change-password-current-password-input :.func--current-password__input)
(def change-password-new-password-input :.func--new-password__input)
(def change-password-submit :.func--change-password__button)
(def change-password-page-body :.func--change-password-page)

(def forgotten-password-button :.func--forgot-password__button)
(def forgotten-password-email :.func--email__input)
(def forgotten-password-submit :.func--send-forgotten-password-email__button)
(def forgotten-password-email-sent-page-body :.func--forgotten-password-confirmation-page)

(def reset-password-field :.func--new-password__input)
(def reset-password-submit :.func--reset-password__button)

(def error-404-page-body :.func--error-404-page)
(def error-500-page-body :.func--error-500-page)

(def css-link [:link (html/attr= :type "text/css")])

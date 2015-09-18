(ns stonecutter.renderer.register-form
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def first-name-form-row-element-selector :.clj--registration-first-name)
(def first-name-validation-element-selector :.clj--registration-first-name__validation)
(def first-name-input-element-selector :.clj--registration-first-name__input)

(def last-name-form-row-element-selector :.clj--registration-last-name)
(def last-name-validation-element-selector :.clj--registration-last-name__validation)
(def last-name-input-element-selector :.clj--registration-last-name__input)

(def email-address-form-row-element-selector :.clj--registration-email)
(def email-address-validation-element-selector :.clj--registration-email__validation)
(def email-address-input-element-selector :.clj--registration-email__input)

(def password-form-row-element-selector :.clj--registration-password)
(def password-validation-element-selector :.clj--registration-password__validation)
(def password-input-element-selector :.clj--registration-password__input)

(def translations (t/load-client-translations))

(defn get-translated-message [key]
  (-> translations :index key))

(def error-to-message
  {:first-name {:blank (get-translated-message :register-first-name-blank-validation-message)
                :too-long (get-translated-message :register-first-name-too-long-validation-message)}
   :last-name {:blank (get-translated-message :register-last-name-blank-validation-message)
               :too-long (get-translated-message :register-last-name-too-long-validation-message)}
   :email-address {:invalid (get-translated-message :register-email-address-invalid-validation-message)
                   :too-long (get-translated-message :register-email-address-too-long-validation-message)}
   :password {:blank (get-translated-message :register-password-blank-validation-message)
              :too-short (get-translated-message :register-password-too-short-validation-message)
              :too-long (get-translated-message :register-password-too-long-validation-message)}})

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

(defn add-or-remove-class! [element-selector css-class add?]
  (let [element (dm/sel1 element-selector)]
    (if add?
      (d/add-class! element css-class)
      (d/remove-class! element css-class))))

(defn set-valid-class! [state field-key form-row-element-selector]
  (let [valid? (boolean (get-in state [field-key :tick]))]
    (add-or-remove-class! form-row-element-selector field-valid-class valid?)))

(defn set-invalid-class! [state field-key form-row-element-selector]
  (let [invalid? (boolean (get-in state [field-key :error]))]
    (add-or-remove-class! form-row-element-selector field-invalid-class invalid?)))

(defn set-error-message! [state field-key validation-element-selector]
  (let [error-key (get-in state [field-key :error])
        message (get-in error-to-message [field-key error-key])]
    (d/set-text! (dm/sel1 validation-element-selector) message)))

(defn render-password-error! [state]
  (set-invalid-class! state :password password-form-row-element-selector)
  (set-valid-class! state :password password-form-row-element-selector)
  (set-error-message! state :password password-validation-element-selector)
  state)

(defn render-email-address-error! [state]
  (set-invalid-class! state :email-address email-address-form-row-element-selector)
  (set-error-message! state :email-address email-address-validation-element-selector)
  state)

(defn render-last-name-error! [state]
  (set-invalid-class! state :last-name last-name-form-row-element-selector)
  (set-error-message! state :last-name last-name-validation-element-selector)
  state)

(defn render-first-name-error! [state]
  (set-invalid-class! state :first-name first-name-form-row-element-selector)
  (set-error-message! state :first-name first-name-validation-element-selector)
  state)

(defn render! [state]
  (-> state
      render-first-name-error!
      render-last-name-error!
      render-email-address-error!
      render-password-error!))
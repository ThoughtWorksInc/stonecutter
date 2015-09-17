(ns stonecutter.renderer.register-form
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def first-name-form-row-element-selector :.clj--registration-first-name)
(def last-name-form-row-element-selector :.clj--registration-last-name)
(def email-address-form-row-element-selector :.clj--registration-email)
(def password-form-row-element-selector :.clj--registration-password)

(def field-invalid-class "form-row--invalid")
(def field-valid-class "form-row--valid")

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

(defn render-password-error! [state]
  (set-invalid-class! state :password password-form-row-element-selector)
  (set-valid-class! state :password password-form-row-element-selector)
  state)

(defn render-email-address-error! [state]
  (set-invalid-class! state :email-address email-address-form-row-element-selector)
  state)

(defn render-last-name-error! [state]
  (set-invalid-class! state :last-name last-name-form-row-element-selector)
  state)

(defn render-first-name-error! [state]
  (set-invalid-class! state :first-name first-name-form-row-element-selector)
  state)

(defn render! [state]
  (-> state
      render-first-name-error!
      render-last-name-error!
      render-email-address-error!
      render-password-error!))
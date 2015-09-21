(ns stonecutter.dom.register-form
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def register-form-element-selector :.clj--register__form)

(def selectors
  {:registration-first-name {:input      :.clj--registration-first-name__input
                             :form-row   :.clj--registration-first-name
                             :validation :.clj--registration-first-name__validation}
   :registration-last-name  {:input      :.clj--registration-last-name__input
                             :form-row   :.clj--registration-last-name
                             :validation :.clj--registration-last-name__validation}
   :registration-email      {:input      :.clj--registration-email__input
                             :form-row   :.clj--registration-email
                             :validation :.clj--registration-email__validation}
   :registration-password   {:input      :.clj--registration-password__input
                             :form-row   :.clj--registration-password
                             :validation :.clj--registration-password__validation}})

(defn form-row-selector [field-key]
  (get-in selectors [field-key :form-row]))

(defn input-selector [field-key]
  (get-in selectors [field-key :input]))

(defn validation-selector [field-key]
  (get-in selectors [field-key :validation]))

(def translations (t/load-client-translations))

(def form-state (atom {:registration-first-name {:value nil :error nil}
                       :registration-last-name  {:value nil :error nil}
                       :registration-email      {:value nil :error nil}
                       :registration-password   {:value nil :error nil :tick false}}))

(def error-field-order [:registration-first-name :registration-last-name :registration-email :registration-password])

(defn get-translated-message [key]
  (-> translations :index key))

(def error-to-message
  {:registration-first-name {:blank    (get-translated-message :register-first-name-blank-validation-message)
                             :too-long (get-translated-message :register-first-name-too-long-validation-message)}
   :registration-last-name  {:blank    (get-translated-message :register-last-name-blank-validation-message)
                             :too-long (get-translated-message :register-last-name-too-long-validation-message)}
   :registration-email      {:invalid  (get-translated-message :register-email-address-invalid-validation-message)
                             :too-long (get-translated-message :register-email-address-too-long-validation-message)}
   :registration-password   {:blank     (get-translated-message :register-password-blank-validation-message)
                             :too-short (get-translated-message :register-password-too-short-validation-message)
                             :too-long  (get-translated-message :register-password-too-long-validation-message)}})

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
  (set-invalid-class! state :registration-password (form-row-selector :registration-password))
  (set-valid-class! state :registration-password (form-row-selector :registration-password))
  (set-error-message! state :registration-password (validation-selector :registration-password))
  state)

(defn render-email-address-error! [state]
  (set-invalid-class! state :registration-email (form-row-selector :registration-email))
  (set-error-message! state :registration-email (validation-selector :registration-email))
  state)

(defn render-last-name-error! [state]
  (set-invalid-class! state :registration-last-name (form-row-selector :registration-last-name))
  (set-error-message! state :registration-last-name (validation-selector :registration-last-name))
  state)

(defn render-first-name-error! [state]
  (set-invalid-class! state :registration-first-name (form-row-selector :registration-first-name))
  (set-error-message! state :registration-first-name (validation-selector :registration-first-name))
  state)

(defn render! [state]
  (-> state
      render-first-name-error!
      render-last-name-error!
      render-email-address-error!
      render-password-error!))

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       input-selector))

(defn get-value [field-key]
  (d/value (dm/sel1 (input-selector field-key))))

(defn update-state-with-value! [field-key]
  (let [value (get-value field-key)]
    (swap! form-state #(assoc-in % [field-key :value] value))))


(defn update-state-with-controller-fn! [func]
  (swap! form-state func))

(defn update-state-and-render! [field-key controller-fn]
  (update-state-with-value! field-key)
  (update-state-with-controller-fn! controller-fn)
  (render! @form-state))

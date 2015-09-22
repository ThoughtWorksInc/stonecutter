(ns stonecutter.js.dom.register-form
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def error-field-order [:registration-first-name :registration-last-name :registration-email :registration-password])

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

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


(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       input-selector))

(defn get-value [field-key]
  (d/value (dm/sel1 (input-selector field-key))))


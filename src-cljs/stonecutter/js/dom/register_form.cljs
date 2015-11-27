(ns stonecutter.js.dom.register-form
  (:require [stonecutter.js.dom.common :as dom]))

(def register-form-element-selector :.clj--register__form)

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

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

(defn get-value [field-key]
  (dom/get-value (input-selector field-key)))


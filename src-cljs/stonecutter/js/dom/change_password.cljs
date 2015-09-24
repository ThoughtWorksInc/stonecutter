(ns stonecutter.js.dom.change-password
  (:require [stonecutter.js.dom.common :as dom]))

(def change-password-form-element-selector :.clj--change-password__form)

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

(def selectors
  {:current-password {:input      :.clj--current-password__input
                      :form-row   :.clj--current-password
                      :validation :.clj--current-password__validation}
   :new-password     {:input      :.clj--new-password__input
                      :form-row   :.clj--new-password
                      :validation :.clj--new-password__validation}})

(defn input-selector [field-key]
  (get-in selectors [field-key :input]))

(defn form-row-selector [field-key]
  (get-in selectors [field-key :form-row]))

(defn validation-selector [field-key]
  (get-in selectors [field-key :validation]))

(defn get-translated-message [key]
  (-> dom/translations :change-password-form key))

(defn get-value [field-key]
  (dom/get-value (input-selector field-key)))

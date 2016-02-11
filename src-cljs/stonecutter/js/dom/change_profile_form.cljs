(ns stonecutter.js.dom.change-profile-form
  (:require [stonecutter.js.dom.common :as dom]))

(def field-invalid-class :form-row--invalid)

(def change-name-form-element-selector :.clj--change-name__form)

(def selectors
  {:change-first-name {:input      :.clj--change-first-name__input
                       :form-row   :.clj--first-name
                       :validation :.clj--change-first-name__validation}
   :change-last-name  {:input      :.clj--change-last-name__input
                       :form-row   :.clj--last-name
                       :validation :.clj--change-last-name__validation}})

(defn input-selector [field-key]
  (get-in selectors [field-key :input]))

(defn form-row-selector [field-key]
  (get-in selectors [field-key :form-row]))

(defn get-value [field-key]
  (dom/get-value (input-selector field-key)))

(defn validation-selector [field-key]
  (get-in selectors [field-key :validation]))

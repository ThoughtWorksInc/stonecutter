(ns stonecutter.register-form
  (:require [dommy.core :as d]
            [stonecutter.renderer.register-form :as rfr]
            [stonecutter.controller.register-form :as rfc]
            [stonecutter.validation :as sv])
  (:require-macros [dommy.core :as dm]))

(def form-state (atom {}))

(def field-key-to-input-field
  {:registration-first-name    rfr/first-name-input-element-selector
   :registration-last-name     rfr/last-name-input-element-selector
   :registration-email rfr/email-address-input-element-selector
   :registration-password      rfr/password-input-element-selector})

(def error-field-order [:registration-first-name :registration-last-name :registration-email :registration-password])

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       field-key-to-input-field))

(defn get-value [field-key]
  (d/value (dm/sel1 (field-key field-key-to-input-field))))

(defn update-state-with-value! [field-key]
  (let [value (get-value field-key)]
    (swap! form-state #(assoc-in % [field-key :value] value))))


(defn update-state-with-controller-fn! [func]
  (swap! form-state func))

(defn update-state-and-render! [field-key controller-fn]
  (update-state-with-value! field-key)
  (update-state-with-controller-fn! controller-fn)
  (rfr/render! @form-state))

(defn focus-on-element [sel]
  (when-let [e (dm/sel1 sel)]
    (.focus e)))

(defn block-invalid-submit [submitEvent]
  (let [params {:registration-first-name (get-value :registration-first-name)
                :registration-last-name  (get-value :registration-last-name)
                :registration-email      (get-value :registration-email)
                :registration-password    (get-value :registration-password)}
        err (sv/validate-registration params (constantly false))]
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (update-state-and-render! :registration-first-name rfc/update-first-name-blur)
      (update-state-and-render! :registration-last-name rfc/update-last-name-blur)
      (update-state-and-render! :registration-email rfc/update-email-address-blur)
      (update-state-and-render! :registration-password rfc/update-password-blur)
      (focus-on-element (first-input-with-errors err)))))
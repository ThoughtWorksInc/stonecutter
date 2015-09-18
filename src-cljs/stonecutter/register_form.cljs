(ns stonecutter.register-form
  (:require [dommy.core :as d]
            [stonecutter.renderer.register-form :as rfr])
  (:require-macros [dommy.core :as dm]))


(def form-state (atom {}))

(def field-key-to-input-field
  {:first-name    rfr/first-name-input-element-selector
   :last-name     rfr/last-name-input-element-selector
   :email-address rfr/email-address-input-element-selector
   :password      rfr/password-input-element-selector})

(defn get-value [field-key]
  (d/value (dm/sel1 (field-key field-key-to-input-field))))

(defn update-state-with-value! [field-key]
  (let [value (get-value field-key)]
    (swap! form-state #(assoc-in % [field-key :value] value))))


(defn update-state-with-controller-fn! [func]
  (swap! form-state func))

(defn update-state-and-render! [field-key controller-fn]
  ;(prn "FORM STATE 1" @form-state)
  (update-state-with-value! field-key)
  ;(prn "FORM STATE 2" @form-state)
  (update-state-with-controller-fn! controller-fn)
  ;(prn "FORM STATE 3" @form-state)
  (rfr/render! @form-state))
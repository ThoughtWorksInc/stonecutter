(ns stonecutter.js.change-password
  (:require [dommy.core :as d]
            [stonecutter.validation :as v]
            [stonecutter.js.renderer.change-password :as r])
  (:require-macros [dommy.core :as dm]))

(def current-password-input :#current-password)
(def new-password-input :#new-password)
(def change-password-form :.clj--change-password__form)

(defn input-value [sel]
  (d/value (dm/sel1 sel)))

(def error-to-input-field
  {:current-password current-password-input
   :new-password     new-password-input})

(def error-field-order [:current-password :new-password])

(defn field-values []
  (->> error-to-input-field
       (map (fn [[error-key input-id]] [error-key (input-value input-id)]))
       (into {})))

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       (get error-to-input-field)))

(defn focus-on-element [sel]
  (when-let [e (dm/sel1 sel)]
    (.focus e)))

(defn block-invalid-submit [submitEvent]
  (let [err (v/validate-change-password (field-values) (constantly true))]
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (focus-on-element (first-input-with-errors err)))))

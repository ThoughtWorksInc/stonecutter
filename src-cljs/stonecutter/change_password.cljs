(ns stonecutter.change-password
  (:require [dommy.core :as d]
            [stonecutter.validation :as v])
  (:require-macros [dommy.core :as dm]))

(def current-password-input :#current-password)
(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)
(def new-password-input :#new-password)

(def field-error-class "form-row--validation-error")

(defn update-new-password! [e]
  (let [password (d/value (dm/sel1 new-password-input))]
    (if-let [err (v/validate-password password)]
      (d/remove-class! (dm/sel1 :.form-row__help) "form-row__help--valid")
      (d/add-class! (dm/sel1 :.form-row__help) "form-row__help--valid"))))

(defn input-value [sel]
  (d/value (dm/sel1 sel)))

(def error-to-field
  {:current-password     current-password-input
   :new-password         new-password-input})

(def error-field-order [:current-password :new-password])

(defn field-values []
  (->> error-to-field
       (map (fn [[error-key input-id]] [error-key (input-value input-id)]))
       (into {})))

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       (get error-to-field)))

(defn focus-on-element [sel]
  (when-let [e (dm/sel1 sel)]
    (.focus e)))

(defn toggle-error-class [field-sel err?]
  (if err?
    (d/add-class! (dm/sel1 field-sel) field-error-class)
    (d/remove-class! (dm/sel1 field-sel) field-error-class)))

(defn check-change-password! [submitEvent]
  (let [err (v/validate-change-password (field-values) (constantly true))]
    (toggle-error-class current-password-field (:current-password err))
    (toggle-error-class new-password-field (:new-password err))
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (focus-on-element (first-input-with-errors err)))))

(defn start []
  (when-let [e (dm/sel1 new-password-input)]
    (d/listen! e :input update-new-password!))
  (when-let [e (dm/sel1 :.clj--change-password__form)]
    (d/listen! e :submit check-change-password!)))

(set! (.-onload js/window) start)


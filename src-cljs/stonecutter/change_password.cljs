(ns stonecutter.change-password
  (:require [dommy.core :as d]
            [stonecutter.validation :as v]
            [stonecutter.renderer.change-password :as r]
            [stonecutter.controller.change-password :as c])
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

(defn update-current-password! [e]
  (let [current-password (input-value current-password-input)]
    (when-not (v/validate-password-format current-password)
      (d/remove-class! (dm/sel1 r/current-password-field) r/field-invalid-class))))

(defn update-new-password! [e]
  (let [current-password (input-value current-password-input)
        new-password (input-value new-password-input)]
    (if (or (v/validate-password-format new-password)
            (v/validate-passwords-are-different current-password new-password))
      (d/remove-class! (dm/sel1 r/new-password-field) r/field-valid-class)
      (do
        (d/add-class! (dm/sel1 r/new-password-field) r/field-valid-class)
        (d/remove-class! (dm/sel1 r/new-password-field) r/field-invalid-class)))))

(defn check-current-password! [e]
  (let [err (v/validate-password-format (input-value current-password-input))]
    (r/update-inline-message r/form-row-current-password-error-class r/error-to-message {:current-password err})
    (r/toggle-invalid-class r/current-password-field err)))

(defn check-new-password! [e]
  (let [current-password (input-value current-password-input)
        new-password (input-value new-password-input)
        err (or (v/validate-password-format new-password)
                (v/validate-passwords-are-different current-password new-password))]
    (r/update-inline-message r/form-row-new-password-error-class r/error-to-message {:new-password err})
    (r/toggle-error-class r/new-password-field err)))

(defn block-invalid-submit [submitEvent]
  (let [err (v/validate-change-password (field-values) (constantly true))]
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (focus-on-element (first-input-with-errors err)))))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(def change-password-form-state (atom {}))

(defn update-state-input! [field]
  (let [new-input-value (field (field-values))]
    (swap! change-password-form-state #(assoc-in % [field :value] new-input-value))))

(defn update-state-and-render [field controller-fn]
  (update-state-input! field)
  (swap! change-password-form-state controller-fn)
  (r/render! @change-password-form-state))

(defn start []

  (setup-listener current-password-input :input #(update-state-and-render :current-password (partial c/update-current-password-input)))
  (setup-listener new-password-input :input update-new-password!)
  (setup-listener current-password-input :input update-new-password!)
  (setup-listener current-password-input :blur #(update-state-and-render :current-password (partial c/update-current-password-blur)))
  (setup-listener new-password-input :blur check-new-password!)

  (setup-listener change-password-form :submit block-invalid-submit))

(set! (.-onload js/window) start)
(ns stonecutter.change-password
  (:require [dommy.core :as d]
            [stonecutter.validation :as v])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def translations (t/load-client-translations))

(defn get-translated-message [key]
  (-> translations :change-password-form key))

(def current-password-input :#current-password)
(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)
(def new-password-input :#new-password)
(def change-password-form :.clj--change-password__form)

(def form-row-new-password-error-class :.cljs--new-password-form-row__inline-message)
(def form-row-current-password-error-class :.cljs--current-password-form-row__inline-message)

(def field-valid-class "form-row--valid")
(def field-invalid-class "form-row--invalid")

(def form-row-validation "form-row__validation")
(def form-row-help "form-row__help")

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

(defn toggle-error-class [field-sel err?]
  (if err?
    (do (d/add-class! (dm/sel1 field-sel) field-invalid-class)
        (d/remove-class! (dm/sel1 field-sel) field-valid-class))
    (do (d/remove-class! (dm/sel1 field-sel) field-invalid-class)
        (d/add-class! (dm/sel1 field-sel) field-valid-class))))

(defn toggle-invalid-class [field-sel err?]
  (if err?
    (d/add-class! (dm/sel1 field-sel) field-invalid-class)
    (d/remove-class! (dm/sel1 field-sel) field-invalid-class)))

(def error-to-message
  {:current-password {:blank     (get-translated-message :current-password-blank-validation-message)
                      :too-short (get-translated-message :current-password-too-short-validation-message)}
   :new-password     {:blank     (get-translated-message :new-password-blank-validation-message)
                      :unchanged (get-translated-message :new-password-unchanged-validation-message)
                      :too-short (get-translated-message :new-password-too-short-validation-message)}})

(defn update-inline-message [field message-map error-map]
  (let [message (get-in message-map (first (seq error-map)))
        element (dm/sel1 field)]
    (d/add-class! element form-row-validation)
    (d/remove-class! element form-row-help)
    (d/set-text! element message)))

(defn update-current-password! [e]
  (let [current-password (input-value current-password-input)]
    (when-not (v/validate-password-format current-password)
      (d/remove-class! (dm/sel1 current-password-field) field-invalid-class))))

(defn update-new-password! [e]
  (let [current-password (input-value current-password-input)
        new-password (input-value new-password-input)]
    (if (or (v/validate-password-format new-password)
            (v/validate-passwords-are-different current-password new-password))
      (d/remove-class! (dm/sel1 new-password-field) field-valid-class)
      (do
        (d/add-class! (dm/sel1 new-password-field) field-valid-class)
        (d/remove-class! (dm/sel1 new-password-field) field-invalid-class)))))

(defn check-current-password! [e]
  (let [err (v/validate-password-format (input-value current-password-input))]
    (update-inline-message form-row-current-password-error-class error-to-message {:current-password err})
    (toggle-invalid-class current-password-field err)))

(defn check-new-password! [e]
  (let [current-password (input-value current-password-input)
        new-password (input-value new-password-input)
        err (or (v/validate-password-format new-password)
                (v/validate-passwords-are-different current-password new-password))]
    (update-inline-message form-row-new-password-error-class error-to-message {:new-password err})
    (toggle-error-class new-password-field err)))

(defn block-invalid-submit [submitEvent]
  (let [err (v/validate-change-password (field-values) (constantly true))]
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (focus-on-element (first-input-with-errors err)))))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(defn start []
  (setup-listener current-password-input :input update-current-password!)
  (setup-listener new-password-input :input update-new-password!)
  (setup-listener current-password-input :input update-new-password!)
  (setup-listener current-password-input :blur check-current-password!)
  (setup-listener new-password-input :blur check-new-password!)
  (setup-listener change-password-form :submit block-invalid-submit))

(set! (.-onload js/window) start)
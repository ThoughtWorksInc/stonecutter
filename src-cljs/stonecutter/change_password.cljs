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

(def error-list :.validation-summary__list)
(def display-error-list-class "validation-summary--show")

(def field-error-class "form-row--validation-error")
(def field-valid-class "form-row--valid")
(def field-invalid-class "form-row--invalid")
(def valid-row-class "form-row__help--valid")

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

(defn append-error-message [field message]
  (let [parent (dm/sel1 field)
        child (-> (d/create-element :li)
                  (d/set-text! message)
                  (d/add-class! "validation-summary__item"))]
    (d/append! parent child)))

(def error-to-message
  {:current-password {:blank     (get-translated-message :current-password-blank-validation-message)
                      :too-short (get-translated-message :current-password-too-short-validation-message)}
   :new-password     {:blank     (get-translated-message :new-password-blank-validation-message)
                      :unchanged (get-translated-message :new-password-unchanged-validation-message)
                      :too-short (get-translated-message :new-password-too-short-validation-message)}})

(defn append-password-error-message [field message-m err]
  (if (empty? err)
    (d/remove-class! (dm/sel1 :.validation-summary) display-error-list-class)
    (do
      (d/add-class! (dm/sel1 :.validation-summary) display-error-list-class)
      (doseq [[input-field error] err]
        (let [message (get-in message-m [input-field error])]
          (append-error-message field message))))))

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
    (toggle-invalid-class current-password-field err)))

(defn check-change-password! [submitEvent]
  (let [err (v/validate-change-password (field-values) (constantly true))]
    (d/clear! (dm/sel1 error-list))
    (append-password-error-message error-list error-to-message err)
    (toggle-invalid-class current-password-field (:current-password err))
    (toggle-error-class new-password-field (:new-password err))
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
  (setup-listener change-password-form :submit check-change-password!))

(set! (.-onload js/window) start)
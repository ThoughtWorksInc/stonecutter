(ns stonecutter.js.renderer.change-password
  (:require [stonecutter.js.dom.common :as dom])
  (:require-macros [stonecutter.translation :as t]))

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

(def form-row-validation "form-row__validation")

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)

(def translations (t/load-client-translations))

(defn get-translated-message [key]
  (-> translations :change-password-form key))

(def error-to-message
  {:current-password {:blank     (get-translated-message :current-password-invalid-validation-message)
                      :too-short (get-translated-message :current-password-invalid-validation-message)
                      :too-long  (get-translated-message :current-password-invalid-validation-message)
                      :invalid   (get-translated-message :current-password-invalid-validation-message)}
   :new-password     {:blank     (get-translated-message :new-password-blank-validation-message)
                      :unchanged (get-translated-message :new-password-unchanged-validation-message)
                      :too-short (get-translated-message :new-password-too-short-validation-message)
                      :too-long  (get-translated-message :new-password-too-long-validation-message)}})

(defn add-class! [selector css-class]
  (dom/add-class! selector css-class))

(defn toggle-class [selector add? class]
  (if add?
    (dom/add-class! selector class)
    (dom/remove-class! selector class)))

(defn toggle-valid-class [field-sel err?]
  (toggle-class field-sel err? field-valid-class))

(defn set-valid-class! [state-map field-key form-row-element-selector]
  (let [valid? (boolean (get-in state-map [field-key :tick]))]
    (if valid?
      (dom/add-class! form-row-element-selector field-valid-class)
      (dom/remove-class! form-row-element-selector field-valid-class))))

(defn set-invalid-class! [state field-key form-row-element-selector]
  (let [invalid? (boolean (get-in state [field-key :error]))]
    (if invalid?
      (dom/add-class! form-row-element-selector field-invalid-class)
      (dom/remove-class! form-row-element-selector field-invalid-class))))

(defn set-error-message! [state field-key validation-element-selector]
  (let [error-key (get-in state [field-key :error])
        message (get-in error-to-message [field-key error-key])]
    (dom/set-text! validation-element-selector message)))

(defn render-current-password-error! [state-map]
  (set-invalid-class! state-map :current-password (form-row-selector :current-password))
  (set-error-message! state-map :current-password (validation-selector :current-password)))

(defn render-new-password-error-and-tick! [state-map]
  (set-invalid-class! state-map :new-password (form-row-selector :new-password))
  (set-valid-class! state-map :new-password (form-row-selector :new-password))
  (set-error-message! state-map :new-password (validation-selector :new-password)))

(defn render-new-password-tick! [state]
  (let [tick (get-in state [:new-password :tick])]
    (toggle-valid-class new-password-field tick))
  state)

(defn render! [state-map]
  (render-current-password-error! state-map)
  (render-new-password-error-and-tick! state-map))

(defn get-value [field-key]
  (dom/get-value (input-selector field-key)))

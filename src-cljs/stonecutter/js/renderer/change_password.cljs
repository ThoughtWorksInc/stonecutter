(ns stonecutter.js.renderer.change-password
  (:require [stonecutter.js.dom.common :as dom])
  (:require-macros [stonecutter.translation :as t]))

(def selectors
  {:current-password {:input      :.clj--current-password__input
                      :form-row   :.clj--current-password
                      :validation :.clj--current-password__validation}
   :new-password     {:input      :.clj--new-password__input
                      :form-row   :.clj--new-password
                      :validation   :.clj--new-password__validation}})

(defn input-selector [field-key]
  (get-in selectors [field-key :input]))

(defn form-row-selector [field-key]
  (get-in selectors [field-key :form-row]))

(defn validation-selector [field-key]
  (get-in selectors [field-key :validation]))

(def form-row-validation "form-row__validation")
(def form-row-help "form-row__help")

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)

(def form-row-new-password-error-class :.cljs--new-password-form-row__inline-message)
(def form-row-current-password-error-class :.cljs--current-password-form-row__inline-message)

(def translations (t/load-client-translations))

(defn get-translated-message [key]
  (-> translations :change-password-form key))

(def error-to-message
  {:current-password {:blank     (get-translated-message :current-password-blank-validation-message)
                      :too-short (get-translated-message :current-password-too-short-validation-message)}
   :new-password     {:blank     (get-translated-message :new-password-blank-validation-message)
                      :unchanged (get-translated-message :new-password-unchanged-validation-message)
                      :too-short (get-translated-message :new-password-too-short-validation-message)}})

(defn update-inline-message [selector message-map error-map]
  (let [message (get-in message-map (first (seq error-map)))]
    (dom/add-class! selector form-row-validation)
    (dom/remove-class! selector form-row-help)
    (dom/set-text! selector message)))

(defn add-class! [selector css-class]
  (dom/add-class! selector css-class))

(defn toggle-class [selector add? class]
  (if add?
    (dom/add-class! selector class)
    (dom/remove-class! selector class)))

(defn toggle-invalid-class [field-sel err?]
  (toggle-class field-sel err? field-invalid-class))

(defn toggle-valid-class [field-sel err?]
  (toggle-class field-sel err? field-valid-class))

(defn render-current-password-error! [state]
  (let [error (get-in state [:current-password :error])]
    (update-inline-message form-row-current-password-error-class error-to-message {:current-password error})
    (toggle-invalid-class current-password-field error)
    state))

(defn render-new-password-error! [state]
  (let [error (get-in state [:new-password :error])]
    (update-inline-message form-row-new-password-error-class error-to-message {:new-password error})
    (toggle-invalid-class new-password-field error)
    state))

(defn render-new-password-tick! [state]
  (let [tick (get-in state [:new-password :tick])]
    (toggle-valid-class new-password-field tick))
  state)

(defn render! [state]
  (-> state
      render-current-password-error!
      render-new-password-error!
      render-new-password-tick!))

(defn get-value [field-key]
  (dom/get-value (input-selector field-key)))

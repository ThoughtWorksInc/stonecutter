(ns stonecutter.renderer.change-password
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def form-row-validation "form-row__validation")
(def form-row-help "form-row__help")

(def field-valid-class "form-row--valid")
(def field-invalid-class "form-row--invalid")

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

(defn update-inline-message [field message-map error-map]
  (let [message (get-in message-map (first (seq error-map)))
        element (dm/sel1 field)]
    (d/add-class! element form-row-validation)
    (d/remove-class! element form-row-help)
    (d/set-text! element message)))

(defn toggle-class [field-sel add? class]
  (if add?
    (d/add-class! (dm/sel1 field-sel) class)
    (d/remove-class! (dm/sel1 field-sel) class)))

(defn toggle-invalid-class [field-sel err?]
  (toggle-class field-sel err? field-invalid-class))

(defn toggle-valid-class [field-sel err?]
  (toggle-class field-sel err? field-valid-class))

(defn toggle-error-class [field-sel err?]
  (if err?
    (do (d/add-class! (dm/sel1 field-sel) field-invalid-class)
        (d/remove-class! (dm/sel1 field-sel) field-valid-class))
    (do (d/remove-class! (dm/sel1 field-sel) field-invalid-class)
        (d/add-class! (dm/sel1 field-sel) field-valid-class))))

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
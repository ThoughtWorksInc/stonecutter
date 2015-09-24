(ns stonecutter.js.controller.change-password
  (:require [stonecutter.validation :as v]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.dom.change-password :as cpd]))

(def default-state {:current-password {:value nil :error nil}
                    :new-password     {:value nil :error nil :tick false}})

(def error-to-message
  {:current-password {:blank     (cpd/get-translated-message :current-password-invalid-validation-message)
                      :too-short (cpd/get-translated-message :current-password-invalid-validation-message)
                      :too-long  (cpd/get-translated-message :current-password-invalid-validation-message)
                      :invalid   (cpd/get-translated-message :current-password-invalid-validation-message)}
   :new-password     {:blank     (cpd/get-translated-message :new-password-blank-validation-message)
                      :unchanged (cpd/get-translated-message :new-password-unchanged-validation-message)
                      :too-short (cpd/get-translated-message :new-password-too-short-validation-message)
                      :too-long  (cpd/get-translated-message :new-password-too-long-validation-message)}})

(defn update-current-password-blur [state]
  (let [password (get-in state [:current-password :value])
        error (v/validate-password-format password)]
    (assoc-in state [:current-password :error] error)))

(defn update-new-password-blur [state]
  (let [current-password (get-in state [:current-password :value])
        new-password (get-in state [:new-password :value])
        error (or (v/validate-password-format new-password)
                  (v/validate-passwords-are-different current-password new-password))]
    (if error
      (-> state
          (assoc-in [:new-password :tick] false)
          (assoc-in [:new-password :error] error))
      (-> state
          (assoc-in [:new-password :tick] true)
          (assoc-in [:new-password :error] nil)))))

(defn update-new-password-input [state]
  (let [current-password (get-in state [:current-password :value])
        new-password (get-in state [:new-password :value])
        error (or (v/validate-password-format new-password)
                  (v/validate-passwords-are-different current-password new-password))]
    (if error
      (assoc-in state [:new-password :tick] false)
      (-> state
          (assoc-in [:new-password :tick] true)
          (assoc-in [:new-password :error] nil)))))

(defn update-current-password-input [state]
  (let [password (get-in state [:current-password :value])
        error (v/validate-password-format password)
        updated-state (if-not error
                        (assoc-in state [:current-password :error] nil)
                        state)]
    (update-new-password-input updated-state)))

(defn set-valid-class! [state-map field-key form-row-element-selector]
  (let [valid? (boolean (get-in state-map [field-key :tick]))]
    (if valid?
      (dom/add-class! form-row-element-selector cpd/field-valid-class)
      (dom/remove-class! form-row-element-selector cpd/field-valid-class))))

(defn set-invalid-class! [state field-key form-row-element-selector]
  (let [invalid? (boolean (get-in state [field-key :error]))]
    (if invalid?
      (dom/add-class! form-row-element-selector cpd/field-invalid-class)
      (dom/remove-class! form-row-element-selector cpd/field-invalid-class))))

(defn set-error-message! [state field-key validation-element-selector]
  (let [error-key (get-in state [field-key :error])
        message (get-in error-to-message [field-key error-key])]
    (dom/set-text! validation-element-selector message)))

(defn render-current-password-error! [state-map]
  (set-invalid-class! state-map :current-password (cpd/form-row-selector :current-password))
  (set-error-message! state-map :current-password (cpd/validation-selector :current-password)))

(defn render-new-password-error-and-tick! [state-map]
  (set-invalid-class! state-map :new-password (cpd/form-row-selector :new-password))
  (set-valid-class! state-map :new-password (cpd/form-row-selector :new-password))
  (set-error-message! state-map :new-password (cpd/validation-selector :new-password)))

(defn render! [state-map]
  (render-current-password-error! state-map)
  (render-new-password-error-and-tick! state-map))

(defn update-state-with-value! [state field-key]
  (let [value (cpd/get-value field-key)]
    (swap! state #(assoc-in % [field-key :value] value))))

(defn update-state-with-controller-fn! [state func]
  (swap! state func))

(defn update-state-and-render! [state field-key controller-fn]
  (update-state-with-value! state field-key)
  (update-state-with-controller-fn! state controller-fn)
  (render! @state))

(def error-to-input-field
  {:current-password (cpd/input-selector :current-password)
   :new-password     (cpd/input-selector :new-password)})

(def error-field-order [:current-password :new-password])

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       (get error-to-input-field)))

(defn block-invalid-submit [state submitEvent]
  (let [params {:current-password (cpd/get-value :current-password)
                :new-password     (cpd/get-value :new-password)}
        err (v/validate-change-password params (constantly true))]
    (when-not (empty? err)
      (dom/prevent-default-submit! submitEvent)
      (update-state-and-render! state :current-password update-current-password-blur)
      (update-state-and-render! state :new-password update-new-password-blur)
      (dom/focus-on-element! (first-input-with-errors err)))))


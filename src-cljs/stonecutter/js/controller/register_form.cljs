(ns stonecutter.js.controller.register-form
  (:require [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.validation :as v]))

(def default-state {:registration-first-name {:value nil :error nil}
                    :registration-last-name  {:value nil :error nil}
                    :registration-email      {:value nil :error nil}
                    :registration-password   {:value nil :error nil :tick false}})

(def error-to-message
  {:registration-first-name {:blank    (rfd/get-translated-message :register-first-name-blank-validation-message)
                             :too-long (rfd/get-translated-message :register-first-name-too-long-validation-message)}
   :registration-last-name  {:blank    (rfd/get-translated-message :register-last-name-blank-validation-message)
                             :too-long (rfd/get-translated-message :register-last-name-too-long-validation-message)}
   :registration-email      {:blank     (rfd/get-translated-message :register-email-address-blank-validation-message)
                             :invalid  (rfd/get-translated-message :register-email-address-invalid-validation-message)
                             :too-long (rfd/get-translated-message :register-email-address-too-long-validation-message)}
   :registration-password   {:blank     (rfd/get-translated-message :register-password-blank-validation-message)
                             :too-short (rfd/get-translated-message :register-password-too-short-validation-message)
                             :too-long  (rfd/get-translated-message :register-password-too-long-validation-message)}})

(defn update-first-name-blur [state]
  (assoc-in state [:registration-first-name :error]
            (v/validate-registration-name (-> state :registration-first-name :value))))

(defn update-last-name-blur [state]
  (assoc-in state [:registration-last-name :error]
            (v/validate-registration-name (-> state :registration-last-name :value))))

(defn update-email-address-blur [state]
  (let [user-exists?-fn (constantly false)]
    (assoc-in state [:registration-email :error]
              (v/validate-registration-email (-> state :registration-email :value) user-exists?-fn))))

(defn update-password-blur [state]
  (assoc-in state [:registration-password :error]
            (v/validate-password-format (-> state :registration-password :value))))

(defn update-first-name-input [state]
  (assoc-in state [:registration-first-name :error] nil))

(defn update-last-name-input [state]
  (assoc-in state [:registration-last-name :error] nil))

(defn update-email-address-input [state]
  (assoc-in state [:registration-email :error] nil))

(defn update-password-input [state]
  (let [error (v/validate-password-format (-> state :registration-password :value))]
    (if error
      (assoc-in state [:registration-password :tick] false)
      (-> state
          (assoc-in [:registration-password :error] nil)
          (assoc-in [:registration-password :tick] true)))))

(defn set-valid-class! [state-map field-key form-row-element-selector]
  (let [valid? (boolean (get-in state-map [field-key :tick]))]
    (if valid?
      (dom/add-class! form-row-element-selector rfd/field-valid-class)
      (dom/remove-class! form-row-element-selector rfd/field-valid-class))))

(defn set-invalid-class! [state field-key form-row-element-selector]
  (let [invalid? (boolean (get-in state [field-key :error]))]
    (if invalid?
      (dom/add-class! form-row-element-selector rfd/field-invalid-class)
      (dom/remove-class! form-row-element-selector rfd/field-invalid-class))))

(defn set-error-message! [state field-key validation-element-selector]
  (let [error-key (get-in state [field-key :error])
        message (get-in error-to-message [field-key error-key])]
    (dom/set-text! validation-element-selector message)))

(defn render-password-error-and-tick! [state-map]
  (set-invalid-class! state-map :registration-password (rfd/form-row-selector :registration-password))
  (set-valid-class! state-map :registration-password (rfd/form-row-selector :registration-password))
  (set-error-message! state-map :registration-password (rfd/validation-selector :registration-password)))

(defn render-email-address-error! [state-map]
  (set-invalid-class! state-map :registration-email (rfd/form-row-selector :registration-email))
  (set-error-message! state-map :registration-email (rfd/validation-selector :registration-email)))

(defn render-last-name-error! [state-map]
  (set-invalid-class! state-map :registration-last-name (rfd/form-row-selector :registration-last-name))
  (set-error-message! state-map :registration-last-name (rfd/validation-selector :registration-last-name)))

(defn render-first-name-error! [state-map]
  (set-invalid-class! state-map :registration-first-name (rfd/form-row-selector :registration-first-name))
  (set-error-message! state-map :registration-first-name (rfd/validation-selector :registration-first-name)))

(defn render! [state-map]
  (doseq [renderer! [render-first-name-error!
                     render-last-name-error!
                     render-email-address-error!
                     render-password-error-and-tick!]]
    (renderer! state-map)))

(defn update-state-with-value! [state field-key]
  (let [value (rfd/get-value field-key)]
    (swap! state #(assoc-in % [field-key :value] value))))

(defn update-state-with-controller-fn! [state func]
  (swap! state func))

(defn update-state-and-render! [state field-key controller-fn]
  (update-state-with-value! state field-key)
  (update-state-with-controller-fn! state controller-fn)
  (render! @state))

(def error-field-order [:registration-first-name :registration-last-name :registration-email :registration-password])

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       rfd/input-selector))

(defn block-invalid-submit [state submitEvent]
  (let [params {:registration-first-name (rfd/get-value :registration-first-name)
                :registration-last-name  (rfd/get-value :registration-last-name)
                :registration-email      (rfd/get-value :registration-email)
                :registration-password   (rfd/get-value :registration-password)}
        err (v/validate-registration params (constantly false))]
    (when-not (empty? err)
      (dom/prevent-default-submit! submitEvent)
      (update-state-and-render! state :registration-first-name update-first-name-blur)
      (update-state-and-render! state :registration-last-name update-last-name-blur)
      (update-state-and-render! state :registration-email update-email-address-blur)
      (update-state-and-render! state :registration-password update-password-blur)
      (dom/focus-on-element! (first-input-with-errors err)))))

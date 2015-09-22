(ns stonecutter.js.controller.register-form
  (:require [stonecutter.js.dom.register-form :as rf]
            [stonecutter.validation :as v])
  (:require-macros [dommy.core :as dm]))

(def default-state {:registration-first-name {:value nil :error nil}
                    :registration-last-name  {:value nil :error nil}
                    :registration-email      {:value nil :error nil}
                    :registration-password   {:value nil :error nil :tick false}})

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

(defn focus-on-element [sel]
  (when-let [e (dm/sel1 sel)]
    (.focus e)))


(defn render-password-error! [state-map]
  (rf/set-invalid-class! state-map :registration-password (rf/form-row-selector :registration-password))
  (rf/set-valid-class! state-map :registration-password (rf/form-row-selector :registration-password))
  (rf/set-error-message! state-map :registration-password (rf/validation-selector :registration-password))
  state-map)

(defn render-email-address-error! [state-map]
  (rf/set-invalid-class! state-map :registration-email (rf/form-row-selector :registration-email))
  (rf/set-error-message! state-map :registration-email (rf/validation-selector :registration-email))
  state-map)

(defn render-last-name-error! [state-map]
  (rf/set-invalid-class! state-map :registration-last-name (rf/form-row-selector :registration-last-name))
  (rf/set-error-message! state-map :registration-last-name (rf/validation-selector :registration-last-name))
  state-map)

(defn render-first-name-error! [state-map]
  (rf/set-invalid-class! state-map :registration-first-name (rf/form-row-selector :registration-first-name))
  (rf/set-error-message! state-map :registration-first-name (rf/validation-selector :registration-first-name))
  state-map)

(defn render! [state-map]
  (-> state-map
      render-first-name-error!
      render-last-name-error!
      render-email-address-error!
      render-password-error!))

(defn update-state-with-value! [state field-key]
  (let [value (rf/get-value field-key)]
    (swap! state #(assoc-in % [field-key :value] value))))

(defn update-state-with-controller-fn! [state func]
  (swap! state func))

(defn update-state-and-render! [state field-key controller-fn]
  (update-state-with-value! state field-key)
  (update-state-with-controller-fn! state controller-fn)
  (render! @state))

(defn block-invalid-submit [state submitEvent]
  (let [params {:registration-first-name (rf/get-value :registration-first-name)
                :registration-last-name  (rf/get-value :registration-last-name)
                :registration-email      (rf/get-value :registration-email)
                :registration-password   (rf/get-value :registration-password)}
        err (v/validate-registration params (constantly false))]
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (update-state-and-render! state :registration-first-name update-first-name-blur)
      (update-state-and-render! state :registration-last-name update-last-name-blur)
      (update-state-and-render! state :registration-email update-email-address-blur)
      (update-state-and-render! state :registration-password update-password-blur)
      (focus-on-element (rf/first-input-with-errors err)))))

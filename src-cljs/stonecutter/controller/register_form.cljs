(ns stonecutter.controller.register-form
  (:require [stonecutter.validation :as v]))

(defn update-first-name-blur [state]
  (assoc-in state [:first-name :error]
            (v/validate-registration-name (-> state :first-name :value))))

(defn update-last-name-blur [state]
  (assoc-in state [:last-name :error]
            (v/validate-registration-name (-> state :last-name :value))))

(defn update-email-address-blur [state]
  (let [user-exists?-fn (constantly false)]
    (assoc-in state [:email-address :error]
              (v/validate-registration-email (-> state :email-address :value) user-exists?-fn))))

(defn update-password-blur [state]
  (assoc-in state [:password :error]
            (v/validate-password-format (-> state :password :value))))

(defn update-first-name-input [state]
  (assoc-in state [:first-name :error] nil))

(defn update-last-name-input [state]
  (assoc-in state [:last-name :error] nil))

(defn update-email-address-input [state]
  (assoc-in state [:email-address :error] nil))

(defn update-password-input [state]
  (if-let [error (v/validate-password-format (-> state :password :value))]
    (assoc-in state [:password :tick] false)
    (-> state
        (assoc-in [:password :error] nil)
        (assoc-in [:password :tick] true))))

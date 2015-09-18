(ns stonecutter.controller.register-form
  (:require [stonecutter.validation :as v]))

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
  (if-let [error (v/validate-password-format (-> state :registration-password :value))]
    (assoc-in state [:registration-password :tick] false)
    (-> state
        (assoc-in [:registration-password :error] nil)
        (assoc-in [:registration-password :tick] true))))

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

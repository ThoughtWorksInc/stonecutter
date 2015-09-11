(ns stonecutter.controller.change-password
  (:require [stonecutter.validation :as v]))

;; CURRENT PASSWORD
(defn update-current-password-blur [state]
  (let [password (get-in state [:current-password :value])
        error (v/validate-password-format password)]
    (assoc-in state [:current-password :error] error)))

(defn update-current-password-input [state]
  (let [password (get-in state [:current-password :value])
        error (v/validate-password-format password)]
    (if-not error
      (assoc-in state [:current-password :error] nil)
      state)))

;; NEW PASSWORD
(defn update-new-password-blur [state]
  (let [current-password (get-in state [:current-password :value])
        new-password (get-in state [:new-password :value])
        error (or (v/validate-password-format new-password)
                  (v/validate-passwords-are-different current-password new-password))]
    (assoc-in state [:new-password :error] error)))

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


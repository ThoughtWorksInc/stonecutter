(ns stonecutter.renderer.change-password
  (:require [stonecutter.change-password :as cp])
  (:require-macros [stonecutter.translation :as t]))


(defn render-current-password-error! [state]
  (let [error (get-in state [:current-password :error])]
    (cp/update-inline-message cp/form-row-current-password-error-class cp/error-to-message {:current-password error})
    (cp/toggle-invalid-class cp/current-password-field error)
    state))

(defn render-new-password-error! [state]
  (let [error (get-in state [:new-password :error])]
    (cp/update-inline-message cp/form-row-new-password-error-class cp/error-to-message {:new-password error})
    (cp/toggle-invalid-class cp/new-password-field error)
    state))

(defn render-new-password-tick! [state]
  (cp/toggle-error-class cp/new-password-field (not (get-in state [:new-password :tick])))
  state)

(defn render! [state]
  (-> state
      render-current-password-error!
      render-new-password-error!
      render-new-password-tick!))
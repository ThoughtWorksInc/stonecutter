(ns stonecutter.js.controller.register-form
  (:require [stonecutter.js.dom.register-form :as rf]
            [stonecutter.validation :as v])
  (:require-macros [dommy.core :as dm]))

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

(defn block-invalid-submit [submitEvent]
  (let [params {:registration-first-name (rf/get-value :registration-first-name)
                :registration-last-name  (rf/get-value :registration-last-name)
                :registration-email      (rf/get-value :registration-email)
                :registration-password   (rf/get-value :registration-password)}
        err (v/validate-registration params (constantly false))]
    (when-not (empty? err)
      (.preventDefault submitEvent)
      (rf/update-state-and-render! :registration-first-name update-first-name-blur)
      (rf/update-state-and-render! :registration-last-name update-last-name-blur)
      (rf/update-state-and-render! :registration-email update-email-address-blur)
      (rf/update-state-and-render! :registration-password update-password-blur)
      (focus-on-element (rf/first-input-with-errors err)))))

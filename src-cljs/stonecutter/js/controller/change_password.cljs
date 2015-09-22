(ns stonecutter.js.controller.change-password
  (:require [stonecutter.validation :as v]
            [stonecutter.js.renderer.change-password :as cpd]))

(def default-state {:current-password {:value nil :error nil}
                    :new-password     {:value nil :error nil :tick false}})

(defn update-current-password-blur [state]
  (let [password (get-in state [:current-password :value])
        error (v/validate-password-format password)]
    (assoc-in state [:current-password :error] error)))

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

(defn update-current-password-input [state]
  (let [password (get-in state [:current-password :value])
        error (v/validate-password-format password)
        updated-state (if-not error
                        (assoc-in state [:current-password :error] nil)
                        state)]
    (update-new-password-input updated-state)))


(defn render! [state-map]
  (doseq [renderer! [cpd/render-current-password-error!
                     cpd/render-new-password-error!
                     cpd/render-new-password-tick!]]
    (renderer! state-map)))

(defn update-state-with-value! [state field-key]
  (let [value (cpd/get-value field-key)]
    (swap! state #(assoc-in % [field-key :value] value))))

(defn update-state-with-controller-fn! [state func]
  (swap! state func))

(defn update-state-and-render! [state field-key controller-fn]
  (update-state-with-value! state field-key)
  (update-state-with-controller-fn! state controller-fn)
  (render! @state))

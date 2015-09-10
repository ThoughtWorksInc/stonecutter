(ns stonecutter.controller.change-password
  (:require [stonecutter.validation :as v])
  (:require-macros [stonecutter.translation :as t]))

(defn update-current-password-blur [state]
  (let [password (:current-password state)
        error (v/validate-password-format password)]
    (assoc state :error error)))

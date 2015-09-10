(ns stonecutter.controller.change-password
  (:require [stonecutter.validation :as v])
  (:require-macros [stonecutter.translation :as t]))

(defn update-current-password-blur [state]
  (let [current-password-value (:current-password state)]
    (if (= current-password-value "")
      (assoc state :error :blank))))

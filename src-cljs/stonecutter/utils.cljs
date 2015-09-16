(ns stonecutter.utils
  (:require [dommy.core :as d]
            [stonecutter.controller.change-password :as c]
            [stonecutter.change-password :as cp]
            [stonecutter.controller.user-list :as ul])
  (:require-macros [dommy.core :as dm]))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (.log js/console "set up listener for user list")
  (setup-multi-listeners :.clj--user-item__toggle :change ul/update-role)

  (setup-listener cp/current-password-input :input #(cp/update-state-and-render :current-password (partial c/update-current-password-input)))
  (setup-listener cp/new-password-input :input #(cp/update-state-and-render :new-password (partial c/update-new-password-input)))
  (setup-listener cp/current-password-input :input #(cp/update-state-and-render :current-password (partial c/update-new-password-input)))

  (setup-listener cp/current-password-input :blur #(cp/update-state-and-render :current-password (partial c/update-current-password-blur)))
  (setup-listener cp/new-password-input :blur #(cp/update-state-and-render :new-password (partial c/update-new-password-blur)))

  (setup-listener cp/change-password-form :submit cp/block-invalid-submit))

(set! (.-onload js/window) start)
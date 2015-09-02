(ns stonecutter.change-password
      (:require [dommy.core :as d]
                [stonecutter.validation :as v])
      (:require-macros [dommy.core :as dm]))

(defn update-current-password! [e]
      (let [password (d/value (dm/sel1 :#current-password))]
           (if-let [err (v/validate-password password)]
                   (d/add-class! (dm/sel1 :#current-password) "invalid")
                   (d/remove-class! (dm/sel1 :#current-password) "invalid"))))

(defn start []
      (when-let [e (dm/sel1 :#current-password)]
              (d/listen! e :input update-current-password!)))

(set! (.-onload js/window) start)


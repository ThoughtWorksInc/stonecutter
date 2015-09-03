(ns stonecutter.change-password
      (:require [dommy.core :as d]
                [stonecutter.validation :as v])
      (:require-macros [dommy.core :as dm]))

#_(defn update-current-password! [e]
      (let [password (d/value (dm/sel1 :#current-password))]
           (if-let [err (v/validate-password password)]
                   (d/add-class! (dm/sel1 :#current-password) "invalid")
                   (d/remove-class! (dm/sel1 :#current-password) "invalid"))))

(defn update-new-password! [e]
  (let [password (d/value (dm/sel1 :#new-password))]
    (if-let [err (v/validate-password password)]
      (d/remove-class! (dm/sel1 :.form-row__help) "form-row__help--valid")
      (d/add-class! (dm/sel1 :.form-row__help) "form-row__help--valid"))))

(defn start []
  (when-let [e (dm/sel1 :#new-password)]
    (d/listen! e :input update-new-password!)))

(set! (.-onload js/window) start)


(ns stonecutter.change-password
      (:require [dommy.core :as d]
                [stonecutter.validation :as v])
      (:require-macros [dommy.core :as dm]))

#_(defn update-current-password! [e]
      (let [password (d/value (dm/sel1 :#current-password))]
           (if-let [err (v/validate-password password)]
                   (d/add-class! (dm/sel1 :#current-password) "invalid")
                   (d/remove-class! (dm/sel1 :#current-password) "invalid"))))

(def current-password-input :#current-password)
(def current-password-field :.clj--current-password)
(def new-password-field :.clj--new-password)
(def new-password-input :#new-password)
(def verify-password-field :.clj--confirm-new-password)

(def field-error-class "form-row--validation-error")

(defn update-new-password! [e]
  (let [password (d/value (dm/sel1 new-password-input))]
    (if-let [err (v/validate-password password)]
      (d/remove-class! (dm/sel1 :.form-row__help) "form-row__help--valid")
      (d/add-class! (dm/sel1 :.form-row__help) "form-row__help--valid"))))

(defn check-change-password! [e]
  ;(.log js/console "here")
  (let [password (d/value (dm/sel1 new-password-input))]
    (if-let [err (v/validate-password password)]
      (do (.preventDefault e)
          (d/add-class! (dm/sel1 current-password-field) field-error-class)
          (d/add-class! (dm/sel1 new-password-field) field-error-class)
          (d/add-class! (dm/sel1 verify-password-field) field-error-class)
          (.focus (dm/sel1 current-password-input)))
      (d/remove-class! (dm/sel1 new-password-field) field-error-class))))

(defn start []
  (when-let [e (dm/sel1 new-password-input)]
    (d/listen! e :input update-new-password!))
  (when-let [e (dm/sel1 :.clj--change-password__form)]
    (d/listen! e :submit check-change-password!)))

(set! (.-onload js/window) start)


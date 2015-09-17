(ns stonecutter.renderer.register-form
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def first-name-form-row-element-selector :.clj--registration-first-name)
(def last-name-form-row-element-selector :.clj--registration-last-name)

(def field-invalid-class "form-row--invalid")
(def field-valid-class "form-row--valid")

(defn render-last-name-error! [state]
  (if (get-in state [:last-name :error])
    (d/add-class! (dm/sel1 last-name-form-row-element-selector) field-invalid-class)
    (d/remove-class! (dm/sel1 last-name-form-row-element-selector) field-invalid-class))
  state)

(defn render-first-name-error! [state]
  (if (get-in state [:first-name :error])
    (d/add-class! (dm/sel1 first-name-form-row-element-selector) field-invalid-class)
    (d/remove-class! (dm/sel1 first-name-form-row-element-selector) field-invalid-class))
  state)

(defn render! [state]
  (-> state
      render-first-name-error!
      render-last-name-error!))
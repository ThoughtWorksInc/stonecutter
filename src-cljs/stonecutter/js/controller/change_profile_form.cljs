(ns stonecutter.js.controller.change-profile-form
  (:require [stonecutter.js.dom.common :as dom]
            [stonecutter.js.controller.client_translations :as ct]
            [stonecutter.validation :as v]
            [stonecutter.js.dom.change-profile-form :as cpfd]))

(def default-state {:change-first-name {:value nil #_(dom/get-value :.clj--change-first-name__input) :error nil}
                    :change-last-name  {:value nil #_(dom/get-value :.clj--change-last-name__input) :error nil}})

(def error-to-message
  {:change-first-name {:blank    (ct/t (dom/get-lang) :index/register-first-name-blank-validation-message)
                       :too-long (ct/t (dom/get-lang) :index/register-first-name-too-long-validation-message)}
   :change-last-name  {:blank    (ct/t (dom/get-lang) :index/register-last-name-blank-validation-message)
                       :too-long (ct/t (dom/get-lang) :index/register-last-name-too-long-validation-message)}})

(defn update-first-name-blur [state]
  (assoc-in state [:change-first-name :error]
              (v/validate-registration-name (-> state :change-first-name :value))))

(defn update-last-name-blur [state]
  (assoc-in state [:change-last-name :error]
              (v/validate-registration-name (-> state :change-last-name :value))))

(defn update-first-name-input [state]
  (assoc-in state [:change-first-name :error] nil))

(defn update-last-name-input [state]
  (assoc-in state [:change-last-name :error] nil))

(defn set-invalid-class! [state field-key form-row-element-selector]
  (let [invalid? (boolean (get-in state [field-key :error]))]
    (if invalid?
      (dom/add-class! form-row-element-selector cpfd/field-invalid-class)
      (dom/remove-class! form-row-element-selector cpfd/field-invalid-class))))

(defn set-error-message! [state field-key validation-element-selector]
  (let [error-key (get-in state [field-key :error])
        message (get-in error-to-message [field-key error-key])]
    (dom/set-text! validation-element-selector message)))

(defn render-last-name-error! [state-map]
  (set-invalid-class! state-map :change-last-name :.clj--last-name)
  (set-error-message! state-map :change-last-name :.clj--change-last-name__validation))

(defn render-first-name-error! [state-map]
  (set-invalid-class! state-map :change-first-name :.clj--first-name)
  (set-error-message! state-map :change-first-name :.clj--change-first-name__validation))

(defn render! [state-map]
  (doseq [renderer! [render-first-name-error!
                     render-last-name-error!]]
    (renderer! state-map)))

(defn update-state-with-value! [state field-key]
  (let [value (cpfd/get-value field-key)]
    (swap! state #(assoc-in % [field-key :value] value))))

(defn update-state-with-controller-fn! [state func]
  (swap! state func))

(defn update-state-and-render! [state field-key controller-fn]
  (update-state-with-value! state field-key)
  (update-state-with-controller-fn! state controller-fn)
  (render! @state))

(def error-field-order [:change-first-name :change-last-name])

(defn first-input-with-errors [err]
  (->> error-field-order
       (filter #(get err %))
       first
       cpfd/input-selector))

(defn block-invalid-submit [state submit-event]
  (let [first-name (cpfd/get-value :change-first-name)
        last-name (cpfd/get-value :change-last-name)
        err (v/validate-change-name first-name last-name)]
    (when-not (empty? err)
      (dom/prevent-default-submit! submit-event)
      (update-state-and-render! state :change-first-name update-first-name-blur)
      (update-state-and-render! state :change-last-name update-last-name-blur)
      (dom/focus-on-element! (first-input-with-errors err)))))
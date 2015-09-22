(ns stonecutter.js.dom.register-form
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def register-form-element-selector :.clj--register__form)

(def field-invalid-class :form-row--invalid)
(def field-valid-class :form-row--valid)

(def selectors
  {:registration-first-name {:input      :.clj--registration-first-name__input
                             :form-row   :.clj--registration-first-name
                             :validation :.clj--registration-first-name__validation}
   :registration-last-name  {:input      :.clj--registration-last-name__input
                             :form-row   :.clj--registration-last-name
                             :validation :.clj--registration-last-name__validation}
   :registration-email      {:input      :.clj--registration-email__input
                             :form-row   :.clj--registration-email
                             :validation :.clj--registration-email__validation}
   :registration-password   {:input      :.clj--registration-password__input
                             :form-row   :.clj--registration-password
                             :validation :.clj--registration-password__validation}})

(defn form-row-selector [field-key]
  (get-in selectors [field-key :form-row]))

(defn input-selector [field-key]
  (get-in selectors [field-key :input]))

(defn validation-selector [field-key]
  (get-in selectors [field-key :validation]))

(def translations (t/load-client-translations))

(defn get-translated-message [key]
  (-> translations :index key))

(defn add-class! [selector css-class]
  (d/add-class! (dm/sel1 selector) css-class))

(defn remove-class! [selector css-class]
  (d/remove-class! (dm/sel1 selector) css-class))

(defn set-text! [selector message]
  (d/set-text! (dm/sel1 selector) message))

(defn focus-on-element! [sel]
  (when-let [e (dm/sel1 sel)]
    (.focus e)))

(defn prevent-default-submit! [submitEvent]
  (.preventDefault submitEvent))

(defn get-value [field-key]
  (d/value (dm/sel1 (input-selector field-key))))


(ns stonecutter.test.test-utils
  (:require [dommy.core :as dommy]
            [clojure.string :as string])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]))

(defn create-event [event-type]
  (let [event (.createEvent js/document "Event")]
    (.initEvent event (name event-type) true true)
    event))

(defn fire!
  "Creates an event of type `event-type`, optionally having
   `update-event!` mutate and return an updated event object,
   and fires it on `node`.
   Only works when `node` is in the DOM"
  [node event-type & [update-event!]]
  (let [update-event! (or update-event! identity)]
    (if (.-createEvent js/document)
      (let [event (create-event event-type)]
        (.dispatchEvent node (update-event! event)))
      (.fireEvent node (str "on" (name event-type))
                  (update-event! (.createEventObject js/document))))))

(defn print-element
  ([message e]
   (print (str message ": " (.-outerHTML e)))
   e)
  ([e] (print-element "" e)))

(defn set-value [sel text]
  (dommy/set-value! (sel1 sel) text))

(defn enter-text [sel text]
  (set-value sel text)
  (fire! (sel1 sel) :input))

(defn lose-focus [sel]
  (fire! (sel1 sel) :blur))

(defn test-field-class-existance [has-class? selector css-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) css-class))
      (if has-class?
        (str "field: " selector " does not contain expected class: " css-class)
        (str "field: " selector " contains class " css-class " when it shouldn't"))))

(def test-field-doesnt-have-class (partial test-field-class-existance false))
(def test-field-has-class (partial test-field-class-existance true))

(defn press-submit [form-sel]
  (fire! (sel1 form-sel) :submit))

(defn has-focus? [sel]
  (is (= (sel1 sel) (.-activeElement js/document))
      (str "Element " sel " does not have focus")))

(defn default-prevented? [event required]
  (is (= (.-defaultPrevented event) required)
      (str "Element " event " should have default prevented of " required)))

(defn has-message-on-selector [selector message]
  (let [selected-elements (sel selector)
        err-messages (mapv (partial dommy/text) selected-elements)]
    (is (not (string/blank? message)) "Error message key returns blank string")
    (is (some #{message} err-messages)
        (str "Missing error message " message " when error occurs"))))

(defn has-no-message-on-selector [selector]
  (let [error-message (dommy/text (sel1 selector))]
    (is (= "" error-message)
        (str "Expecting no error message but received: " error-message))))

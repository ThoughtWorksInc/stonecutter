(ns stonecutter.test.test-utils
  (:require [cemerick.cljs.test]
            [dommy.core :as dommy]
            [clojure.string :as string])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]))

(defn print-element
  ([message e]
   (print (str message ": " (.-outerHTML e)))
   e)
  ([e] (print-element "" e)))

(defn string-of-length [n]
  (apply str (repeat n "x")))

(defn set-html! [html-string]
  (dommy/set-html! (sel1 :html) html-string))

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

(defn fire-change-event! [sel]
  (fire! (sel1 sel) :change))

(defn set-value [sel text]
  (dommy/set-value! (sel1 sel) text))

(defn enter-text [sel text]
  (set-value sel text)
  (fire! (sel1 sel) :input))

(defn press-submit [form-sel]
  (fire! (sel1 form-sel) :submit))

(defn lose-focus [sel]
  (fire! (sel1 sel) :blur))

(defn move-mouse-over-elem [sel]
  (fire! (sel1 sel) :mouseover))

(defn move-mouse-out-of-elem [sel]
  (fire! (sel1 sel) :mouseout))

(defn test-field-class-existance [has-class? selector css-class]
  (is (= has-class? (dommy/has-class? (sel1 selector) css-class))
      (if has-class?
        (str "field: " selector " does not contain expected class: " css-class)
        (str "field: " selector " contains class " css-class " when it shouldn't"))))

(def test-field-doesnt-have-class (partial test-field-class-existance false))
(def test-field-has-class (partial test-field-class-existance true))

(defn test-field-attr-existance [has-attr? selector attr]
  (is (= has-attr? (some? (dommy/attr (sel1 selector) attr)))
      (if has-attr?
        (str "field: " selector " does not contain expected attribute: " attr)
        (str "field: " selector " contains attribute " attr " when it shouldn't"))))

(def test-field-doesnt-have-attr (partial test-field-attr-existance false))
(def test-field-has-attr (partial test-field-attr-existance true))

(defn element-has-text [selector expected-text]
  (let [selected-element (sel1 selector)
        text (dommy/text selected-element)]
    (is (not (string/blank? text)) "Element has no text")
    (is (= expected-text text)
        (str "Expected element to have <" expected-text "> but actually found <" text ">"))))

(defn element-has-no-text [selector]
  (let [selected-element (sel1 selector)
        text (dommy/text selected-element)]
    (is (string/blank? text) "Element is not blank")))

(defn has-focus? [sel]
  (is (= (sel1 sel) (.-activeElement js/document))
      (str "Element " sel " does not have focus")))

(defn default-prevented? [event required]
  (is (= (.-defaultPrevented event) required)
      (str "Element " event " should have default prevented of " required)))

(def mock-call-state (atom {}))

(defn reset-mock-call-state! []
  (reset! mock-call-state {}))

(defn mock-add-class! [selector css-class]
  (swap! mock-call-state update-in [:add-class-calls selector] conj css-class))

(defn test-add-class-was-called-with [selector css-class]
  (is (= css-class (some #{css-class} (get-in @mock-call-state [:add-class-calls selector])))
      (str "add-class! was not called with selector: " selector " and css class: " css-class)))

(defn mock-remove-class! [selector css-class]
  (swap! mock-call-state update-in [:remove-class-calls selector] conj css-class))

(defn test-remove-class-was-called-with [selector css-class]
  (is (= css-class (some #{css-class} (get-in @mock-call-state [:remove-class-calls selector])))
      (str "remove-class! was not called with selector: " selector " and css class: " css-class)))

(defn mock-set-text! [selector message]
  (swap! mock-call-state assoc-in [:set-text-calls selector] message))

(defn test-set-text-was-called-with [selector message]
  (is (= message (get-in @mock-call-state [:set-text-calls selector]))
      (str "the last call to set-text! with selector: '" selector "' did not have the message: \"" message "\"")))

(defn mock-submit-form! [selector]
  (swap! mock-call-state assoc :submit-form-calls selector))

(defn test-submit-form-was-called [selector]
  (is (= selector (:submit-form-calls @mock-call-state))
      (str "the last call to submit-form! was not with selector: " selector)))

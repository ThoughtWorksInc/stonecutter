(ns stonecutter.test.change-password
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy]
            [stonecutter.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1]]
                   [stonecutter.test.macros :refer [load-template]]))

(def valid-class "form-row__help--valid")

(def invalid-password "blah")
(def valid-password "12345678")

(defn fire!
      "Creates an event of type `event-type`, optionally having
       `update-event!` mutate and return an updated event object,
       and fires it on `node`.
       Only works when `node` is in the DOM"
      [node event-type & [update-event!]]
      (let [update-event! (or update-event! identity)]
           (if (.-createEvent js/document)
             (let [event (.createEvent js/document "Event")]
                  (.initEvent event (name event-type) true true)
                  (.dispatchEvent node (update-event! event)))
             (.fireEvent node (str "on" (name event-type))
                         (update-event! (.createEventObject js/document))))))

(defn setup-page! [html]
    (dommy/set-html! (sel1 :html) html))

(def change-password-template (load-template "public/change-password.html"))

(defn enter-text [sel text]
      (dommy/set-value! (sel1 sel) text)
      (fire! (sel1 sel) :input))

(defn test-field-has-valid-class [selector valid?]
  (is (= valid? (dommy/has-class? (sel1 selector) valid-class))
      (str "field" selector " does not contain correct class: " valid-class)))

(defn test-field-validates-client-side [selector target-element]
  (test-field-has-valid-class target-element false)
  (enter-text selector valid-password)
  (test-field-has-valid-class target-element true)
  (enter-text selector invalid-password)
  (test-field-has-valid-class target-element false))

(deftest password-validation
         (setup-page! change-password-template)
         (cp/start)
         (test-field-validates-client-side :#new-password :.form-row__help))

(defn run-all []  (run-tests))

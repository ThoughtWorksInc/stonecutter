(ns stonecutter.test.change-password
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy]
            [stonecutter.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1]]
                   [stonecutter.test.macros :refer [load-template]]))

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
;
(def change-password-template (load-template "public/change-password.html"))

(defn enter-text [sel text]
      (dommy/set-value! (dommy/sel1 sel) text)
      (fire! (sel1 sel) :input))

(deftest password-validation
         (setup-page! change-password-template)
         (cp/start)
         (is (= false (dommy/has-class? (sel1 :#current-password) "invalid")))
         (enter-text :#current-password "blah")
         (is (= true (dommy/has-class? (sel1 :#current-password) "invalid")))
         (enter-text :#current-password "12345678")
         (is (= false (dommy/has-class? (sel1 :#current-password) "invalid"))))

(defn run-all []  (run-tests))

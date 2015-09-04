(ns stonecutter.test.test-utils)

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

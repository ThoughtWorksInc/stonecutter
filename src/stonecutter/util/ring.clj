(ns stonecutter.util.ring)

(defn complete-uri-of [request]
    (str (:uri request)
         (when (:query-string request) "?")
         (:query-string request)))

(defn preserve-session [response request]
  (-> response
      (assoc :session (:session request))))

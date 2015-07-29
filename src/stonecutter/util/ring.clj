(ns stonecutter.util.ring)

(defn complete-uri-of [request]
    (str (:uri request)
         (when (:query-string request) "?")
         (:query-string request)))

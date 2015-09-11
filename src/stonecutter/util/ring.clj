(ns stonecutter.util.ring
  (:require [stonecutter.session :as session]))

(defn complete-uri-of [request]
    (str (:uri request)
         (when (:query-string request) "?")
         (:query-string request)))

(defn preserve-session [response request]
  (-> response
      (session/replace-session-with (:session request))))

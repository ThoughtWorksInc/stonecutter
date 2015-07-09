(ns stonecutter.middleware
  (:require [clojure.tools.logging :as log]
            [stonecutter.translation :as t]))

(defn wrap-error-handling [handler err-handler dev-mode?]
  (if-not dev-mode?
    (fn [request]
      (try
        (handler request)
        (catch Exception e
          (log/error e)
          (err-handler request))))
    handler))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (t/translations-fn t/translation-map))
        handler)))

(defn wrap-handlers [handlers wrap-function exclusions]
  (into {} (for [[k v] handlers]
             [k (if (k exclusions) v (wrap-function v))])))

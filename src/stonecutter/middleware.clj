(ns stonecutter.middleware
  (:require [clojure.tools.logging :as log]
            [stonecutter.translation :as translation]
            [stonecutter.helper :as helper]))

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
        (assoc-in [:context :translator] (translation/translations-fn translation/translation-map))
        handler)))

(defn wrap-handlers [handlers wrap-function exclusions]
  (into {} (for [[k v] handlers]
             [k (if (k exclusions) v (wrap-function v))])))

(defn wrap-disable-caching [handler]
  (fn [request]
    (-> request
        handler
        helper/disable-caching)))

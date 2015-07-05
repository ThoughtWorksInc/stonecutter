(ns stonecutter.middleware
  (:require [clojure.tools.logging :as log]
            [stonecutter.view.error :as error]
            [ring.util.response :as r]
            [stonecutter.utils :as u]
            [stonecutter.translation :as t]))

(defn wrap-error-handling [handler dev-mode?]
  (if-not dev-mode?
    (let [context {:translator (t/translations-fn t/translation-map)}]
      (fn [request]
        (try
          (handler request)
          (catch Exception e
            (log/error e)
            (-> (u/html-response (error/internal-server-error context)) (r/status 500))))))
    handler))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (t/translations-fn t/translation-map))
        handler)))
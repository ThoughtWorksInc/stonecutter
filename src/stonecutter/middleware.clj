(ns stonecutter.middleware
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [stonecutter.translation :as translation]
            [stonecutter.routes :as routes]
            [stonecutter.controller.user :as user]
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

(defn wrap-handle-404 [handler error-404-handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:status response) 404)
        (error-404-handler request)
        response))))

(defn wrap-translator [handler]
  (fn [request]
    (-> request
        (assoc-in [:context :translator] (translation/translations-fn translation/translation-map))
        handler)))

(defn wrap-theme [handler theme]
  (fn [request]
    (-> request
        (assoc-in [:context :theme] theme)
        handler)))

(defn wrap-handlers [handlers wrap-function exclusions]
  (into {} (for [[k v] handlers]
             [k (if (k exclusions) v (wrap-function v))])))

(defn wrap-disable-caching [handler]
  (fn [request]
    (-> request
        handler
        helper/disable-caching)))

(defn wrap-signed-in [handler]
  (fn [request]
    (if (user/signed-in? request)
      (handler request)
      (r/redirect (routes/path :sign-in)))))

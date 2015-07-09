(ns stonecutter.helper
  (:require [ring.util.response :as r]))

(defn html-response [s]
  (-> s
      r/response
      (r/content-type "text/html")))

(defn disable-caching [response]
  (-> response
      (assoc-in [:headers "Cache-Control"] "no-cache, no-store, must-revalidate")
      (assoc-in [:headers "Pragma"] "no-cache")
      (assoc-in [:headers "Expires"] 0)))
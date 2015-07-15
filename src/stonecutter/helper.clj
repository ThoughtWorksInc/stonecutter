(ns stonecutter.helper
  (:require [ring.util.response :as r]
            [stonecutter.translation :as t]
            [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]))

(defn enlive-to-str [nodes]
  (->> nodes
       html/emit*
       (apply str)))

(defn enlive-response [enlive-m context]
  (-> enlive-m
      (t/context-translate context)
      enlive-to-str
      r/response
      (r/content-type "text/html")))

(defn disable-caching [response]
  (-> response
      (assoc-in [:headers "Cache-Control"] "no-cache, no-store, must-revalidate")
      (assoc-in [:headers "Pragma"] "no-cache")
      (assoc-in [:headers "Expires"] "0")))
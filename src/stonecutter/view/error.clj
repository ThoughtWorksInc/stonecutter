(ns stonecutter.view.error
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]))

(defn not-found-error [context]
  (vh/transform-template context "public/error-404.html"))

(defn modify-error-message-key [error-key enlive-map]
  (html/at enlive-map [:.clj--error-info] (html/set-attr :data-l8n error-key)))

(defn internal-server-error [context & additional-transformations]
  (apply vh/transform-template context "public/error-500.html" additional-transformations))

(defn csrf-error [context]
  (internal-server-error context (partial modify-error-message-key "content:error-403/page-intro")))

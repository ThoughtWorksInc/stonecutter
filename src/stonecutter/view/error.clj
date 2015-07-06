(ns stonecutter.view.error
  (:require [stonecutter.view.view-helpers :refer [transform-template]]
            [net.cgrand.enlive-html :as html]))

(defn not-found-error [context]
  (transform-template context "public/error-404.html"))

(defn modify-error-message-key [error-key enlive-map]
  (html/at enlive-map [:.clj--error-info] error-key))

(defn internal-server-error [context & additional-transformations]
  (apply transform-template context "public/error-500.html" additional-transformations))

(defn csrf-error [context]
  (internal-server-error context (partial modify-error-message-key "content:error-403/page-intro")))

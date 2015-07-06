(ns stonecutter.view.error
  (:require [stonecutter.view.view-helpers :refer [transform-template]]))

(defn render-error-page [context page-url]
  (transform-template context page-url))

(defn not-found-error [context]
  (render-error-page context "public/error-404.html"))

(defn internal-server-error [context]
  (render-error-page context "public/error-500.html"))

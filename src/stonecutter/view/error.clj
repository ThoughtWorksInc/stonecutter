(ns stonecutter.view.error
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]))

(defn not-found-error []
  (vh/load-template "public/error-404.html"))

(defn- modify-error-message-key [enlive-map error-key]
  (html/at enlive-map [:.clj--error-info] (html/set-attr :data-l8n error-key)))

(defn internal-server-error []
  (vh/load-template "public/error-500.html"))

(defn csrf-error []
  (-> (internal-server-error) (modify-error-message-key "content:error-403/page-intro")))

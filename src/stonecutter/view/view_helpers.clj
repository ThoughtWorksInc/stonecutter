(ns stonecutter.view.view-helpers
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [clojure.tools.logging :as log]
            [stonecutter.translation :as t]))

(defn anti-forgery-snippet []
  (html/html-snippet (anti-forgery-field)))

(defn add-anti-forgery [enlive-m]
  (html/at enlive-m
           [:form] (html/prepend (anti-forgery-snippet))))

(defn remove-element [enlive-m selector]
  (html/at enlive-m selector nil))

(defn set-flash-message [request message-key enlive-m]
  (if (= (:flash request) message-key)
    enlive-m
    (remove-element enlive-m [:.clj--flash-message-container])))

(defn remove-attribute [enlive-m selector attr]
  (html/at enlive-m selector (html/remove-attr attr)))

(defn remove-attribute-globally [enlive-m attr]
  (remove-attribute enlive-m [(html/attr? attr)] attr))

(defn remove-work-in-progress [enlive-m]
  (remove-element enlive-m [:.clj-wip]))

;;; templates

(def template-caching? (atom true))

(def template-cache (atom {}))

(defn enable-template-caching! []
  (swap! template-caching? (constantly true)))

(defn reset-template-cache! []
  (swap! template-cache (constantly {})))

(defn disable-template-caching! []
  (swap! template-caching? (constantly false))
  (reset-template-cache!))

(defn html-resource-with-log [path]
  (log/debug (format "Loading template %s from file" path))
  (html/html-resource path))

(defn load-template [path]
  (if @template-caching?
    (if (contains? @template-cache path)
      (get @template-cache path)
      (let [html (html-resource-with-log path)]
        (swap! template-cache #(assoc % path html))
        html))
    (html-resource-with-log path)))

(defn apply-fns [x fns]
  (reduce #(%2 %1) x fns))

(defn enlive-to-str [nodes]
  (->> nodes
       html/emit*
       (apply str)))

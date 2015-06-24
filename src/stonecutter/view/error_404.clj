(ns stonecutter.view.error-404
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]))

(defn not-found-error [context]
  (let [translator (:translator context)]
  (->> (html/html-resource "public/error-404.html")
       (t/translate translator)
       html/emit*
       (apply str))))

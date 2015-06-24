(ns stonecutter.view.error
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]))

(defn render-error-page [context page-url]
  (let [translator (:translator context)]
  (->> (html/html-resource page-url)
       (t/translate translator)
       html/emit*
       (apply str))))

(defn not-found-error [context]
  (render-error-page context "public/error-404.html"))

(defn internal-server-error [context]
  (render-error-page context "public/error-500.html"))

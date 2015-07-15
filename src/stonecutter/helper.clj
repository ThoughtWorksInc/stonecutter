(ns stonecutter.helper
  (:require [ring.util.response :as r]
            [stonecutter.translation :as t]
            [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]))

(defn update-theme [enlive-m context]
  (prn context)
  (if-let [theme (:theme context)]
    (-> enlive-m
        (html/at [(html/attr= :data-clojure-id "theme-link")] (html/set-attr :href (format "stylesheets/%s_theme.css" theme)))
        (vh/remove-attribute-globally :data-clojure-id))
    enlive-m))

(defn enlive-response [enlive-m context]
  (-> enlive-m
      (update-theme context)
      (t/context-translate context)
      vh/enlive-to-str
      r/response
      (r/content-type "text/html")))

(defn disable-caching [response]
  (-> response
      (assoc-in [:headers "Cache-Control"] "no-cache, no-store, must-revalidate")
      (assoc-in [:headers "Pragma"] "no-cache")
      (assoc-in [:headers "Expires"] "0")))
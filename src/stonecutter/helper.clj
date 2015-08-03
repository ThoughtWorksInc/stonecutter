(ns stonecutter.helper
  (:require [ring.util.response :as r]
            [net.cgrand.enlive-html :as html]
            [stonecutter.translation :as t]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.config :as config]))

(defn update-app-name [enlive-m context]
  (let [app-name (config/app-name (:config-m context))]
    (-> enlive-m
        (html/at [:.clj--app-name] (html/content app-name)))))

(defn set-favicon [enlive-m context]
  (if-let [favicon-file-name (config/favicon-file-name (:config-m context))]
    (-> enlive-m
        (html/at [[:link (html/attr= :rel "shortcut icon")]] (html/set-attr :href (str "/" favicon-file-name))))
    enlive-m))

(defn enlive-response [enlive-m context]
  (-> enlive-m
      (update-app-name context)
      (set-favicon context)
      (t/context-translate context)
      vh/enlive-to-str
      r/response
      (r/content-type "text/html")))

(defn disable-caching [response]
  (-> response
      (assoc-in [:headers "Cache-Control"] "no-cache, no-store, must-revalidate")
      (assoc-in [:headers "Pragma"] "no-cache")
      (assoc-in [:headers "Expires"] "0")))

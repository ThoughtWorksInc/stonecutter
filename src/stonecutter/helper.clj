(ns stonecutter.helper
  (:require [ring.util.response :as r]
            [net.cgrand.enlive-html :as html]
            [stonecutter.translation :as t]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.config :as config]
            [clojure.java.io :as io]))

(defn copy [uri file]
  (let [new-file-path (str "resources/public/" file)]
    (io/make-parents new-file-path)
    (with-open [in (io/input-stream uri)
                out (io/output-stream new-file-path)]
      (io/copy in out))))

(defn update-app-name [enlive-m request]
  (let [app-name (config/app-name (get-in request [:context :config-m]))]
    (-> enlive-m
        (html/at [:.clj--app-name] (html/content app-name)))))


(defn set-favicon [enlive-m request]
  (if-let [favicon-file-name (config/favicon-file-name (get-in request [:context :config-m]))]
    (-> enlive-m
        (html/at [[:link (html/attr= :rel "shortcut icon")]] (html/set-attr :href (str "/" favicon-file-name))))
    enlive-m))

(defn update-attr [attr f & args]
  (fn [node]
    (apply update-in node [:attrs attr] f args)))

(defn prepend-to-attr [attr s]
  (fn [node]
    (update-in node [:attrs attr] #(str s %))))

(defn enlive-response [enlive-m request]
  (-> enlive-m
      (update-app-name request)
      (set-favicon request)
      (t/context-translate request)
      vh/remove-work-in-progress
      vh/enlive-to-str
      r/response
      (r/content-type "text/html")))

(defn disable-caching [response]
  (when response
    (-> response
        (assoc-in [:headers "Cache-Control"] "no-cache, no-store, must-revalidate")
        (assoc-in [:headers "Pragma"] "no-cache")
        (assoc-in [:headers "Expires"] "0"))))
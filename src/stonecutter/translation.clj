(ns stonecutter.translation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [traduki.core :as t]
            [stonecutter.util.map :as map]))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn load-translations-from-file [file-name]
  (-> file-name
      io/resource
      slurp
      load-translations-from-string))

(def translation-map
  (map/deep-merge
    (load-translations-from-file "lang/en.yml")
    (load-translations-from-file "lang/en-client.yml")))

(defmacro load-client-translations []
  (load-translations-from-file "lang/en-client.yml"))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))
          translation (get-in translation-map [key1 key2])]
      (when-not translation (log/warn (str "No translation found for " translation-key)))
      translation)))

(defn context-translate [enlive-m context]
  (t/translate (:translator context) enlive-m))

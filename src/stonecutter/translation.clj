(ns stonecutter.translation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :refer [resource]]))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn load-translations-from-file [file-name]
  (-> file-name
      resource
      slurp
      load-translations-from-string))
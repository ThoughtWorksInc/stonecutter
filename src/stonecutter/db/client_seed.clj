(ns stonecutter.db.client-seed
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.client :as client]))

(defn load-client-credentials-from-string [s]
  (yaml/parse-string s))

(defn load-client-credentials-from-resource [resource-name]
  (-> resource-name
      io/resource
      slurp
      load-client-credentials-from-string))

(defn load-client-credentials-from-file [file-path]
  (-> file-path
      slurp
      load-client-credentials-from-string))

(defn load-client-credentials [resource-or-file]
  (if (io/resource resource-or-file)
    (load-client-credentials-from-resource resource-or-file)
    (load-client-credentials-from-file resource-or-file)))

(defn load-client-credentials-and-store-clients [resource-or-file]
  (do (client/delete-clients! @storage/client-store)
      (client/store-clients-from-map (load-client-credentials resource-or-file))))

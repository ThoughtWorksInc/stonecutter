(ns stonecutter.client
  (:require [clj-yaml.core :as yaml]
            [clauth.client :as client-store]
            [clojure.java.io :refer [resource]]))

(defn load-client-credentials-from-string [s]
  (yaml/parse-string s))

(defn load-client-credentials-from-file [file-name]
  (-> file-name
      resource
      slurp
      load-client-credentials-from-string))

(defn register-clients-from-map [client-credentials-map]
  (let [client-credentials-seq (seq client-credentials-map)]
    (doseq [client-entry client-credentials-seq]
      (client-store/store-client {:name          (:name client-entry)
                                  :client-id     (:id client-entry)
                                  :client-secret (:secret client-entry)
                                  :url           nil}))))
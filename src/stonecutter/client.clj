(ns stonecutter.client
  (:require [clj-yaml.core :as yaml]
            [clauth.client :as client-store]
            [environ.core :refer [env]]
            [schema.core :as schema]
            [clojure.java.io :refer [resource]]))

(def not-empty? (complement empty?))

(def Client
  "A schema for a client entry"
  {:name          (schema/both schema/Str (schema/pred not-empty?))
   :client-id     (schema/both schema/Str (schema/pred not-empty?))
   :client-secret (schema/both schema/Str (schema/pred not-empty?))
   :url           schema/Any})

(defn load-client-credentials-from-string [s]
  (yaml/parse-string s))

(defn load-client-credentials-from-resource [resource-name]
  (-> resource-name
      resource
      slurp
      load-client-credentials-from-string))

(defn load-client-credentials-from-file [file-path]
  (-> file-path
      slurp
      load-client-credentials-from-string))

(defn load-client-credentials [resource-or-file]
  (if (resource resource-or-file)
    (load-client-credentials-from-resource resource-or-file)
    (load-client-credentials-from-file resource-or-file)))

(defn is-not-duplicate-client-id? [clients client-id]
  (not-any? #(= client-id (:client-id %)) clients))

(defn store-clients-from-map [client-credentials-map]
  (let [client-credentials-seq (seq client-credentials-map)]
    (doseq [client-entry client-credentials-seq]
      (schema/validate
        Client
        client-entry)
      (let [name (:name client-entry)
            client-id (:client-id client-entry)
            client-secret (:client-secret client-entry)]
        (when (is-not-duplicate-client-id? (client-store/clients) client-id)
          (client-store/store-client {:name          name
                                      :client-id     client-id
                                      :client-secret client-secret
                                      :url           nil}))))))

(defn load-client-credentials-and-store-clients [file-name]
  (store-clients-from-map (load-client-credentials-from-resource file-name)))
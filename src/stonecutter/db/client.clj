(ns stonecutter.db.client
  (:require [clj-yaml.core :as yaml]
            [clauth.client :as cl-client]
            [schema.core :as schema]
            [clojure.java.io :as io]
            [clojure.string :as s]))

(def not-blank? (complement s/blank?))

(def Client
  "A schema for a client entry"
  {:name          (schema/both schema/Str (schema/pred not-blank?))
   :client-id     (schema/both schema/Str (schema/pred not-blank?))
   :client-secret (schema/both schema/Str (schema/pred not-blank?))
   :url           schema/Any})

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

(defn is-not-duplicate-client-id? [clients client-id]
  (not-any? #(= client-id (:client-id %)) clients))

(defn validate-client-entry [client-entry]
  (schema/validate
    Client
    client-entry))

(defn store-clients-from-map [client-credentials-map]
  (let [client-credentials-seq (seq client-credentials-map)]
    (doseq [client-entry client-credentials-seq]
      (validate-client-entry client-entry)
      (let [name (:name client-entry)
            client-id (:client-id client-entry)
            client-secret (:client-secret client-entry)]
        (when (is-not-duplicate-client-id? (cl-client/clients) client-id)
          (cl-client/store-client {:name          name
                                   :client-id     client-id
                                   :client-secret client-secret
                                   :url           nil}))))))


(defn delete-clients![]
  (cl-client/reset-client-store!))

(defn load-client-credentials-and-store-clients [resource-or-file]
  (do (delete-clients!)
      (store-clients-from-map (load-client-credentials resource-or-file))))

(defn retrieve-client [client-id]
  (cl-client/fetch-client client-id))

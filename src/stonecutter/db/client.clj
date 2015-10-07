(ns stonecutter.db.client
  (:require [clauth.store :as cl-store]
            [clauth.client :as cl-client]
            [schema.core :as schema]
            [clojure.string :as s]
            [crypto.random :as random]))

(def not-blank? (complement s/blank?))

(def Client
  "A schema for a client entry"
  {:name          (schema/both schema/Str (schema/pred not-blank?))
   :client-id     (schema/both schema/Str (schema/pred not-blank?))
   :client-secret (schema/both schema/Str (schema/pred not-blank?))
   :url           (schema/both schema/Str (schema/pred not-blank?))})

(defn validate-url-format [client-entry]
  (let [url (:url client-entry)]
    (if (re-find #"https?://" url)
      client-entry
      (throw Exception "missing resource prefix e.g. https://"))))

(defn validate-client-entry [client-entry]
  (-> (schema/validate Client client-entry)
      validate-url-format))

(defn delete-clients! [client-store]
  (cl-client/reset-client-store! client-store))

(defn retrieve-client [client-store client-id]
  (dissoc (cl-client/fetch-client client-store client-id) :client-secret))

(defn retrieve-clients [client-store]
  (cl-store/entries client-store))

(defn unique-client-id? [client-store client-id]
  (nil? (retrieve-client client-store client-id)))

(defn store-client [client-store name url]
  (let [client-id (random/base32 20)
        client-secret (random/base32 20)]
    (when (unique-client-id? client-store client-id)
      (cl-client/store-client client-store {:name          name
                                            :client-id     client-id
                                            :client-secret client-secret
                                            :url           url}))))

(defn store-clients-from-map [client-store client-credentials-map]
  (let [client-credentials-seq (seq client-credentials-map)]
    (doseq [client-entry client-credentials-seq]
      (validate-client-entry client-entry)
      (let [name (:name client-entry)
            client-id (:client-id client-entry)
            client-secret (:client-secret client-entry)
            url (:url client-entry)]
        (when (unique-client-id? client-store client-id)
          (cl-client/store-client client-store {:name          name
                                                :client-id     client-id
                                                :client-secret client-secret
                                                :url           url}))))))



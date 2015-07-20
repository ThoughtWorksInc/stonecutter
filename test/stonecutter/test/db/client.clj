(ns stonecutter.test.db.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as cl-client]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.client :as c]))

(background
  (before :facts (storage/setup-in-memory-stores!)
          :after (storage/reset-in-memory-stores!)))

(def client-credentials-map
  '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "http://greenparty.org"}
    {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "http://redparty.org"}))

(fact "can store clients using credentials from the map"
      (c/store-clients-from-map client-credentials-map)
      (cl-client/clients) => '({:client-id "NOPQRSTUVWXYZ" :client-secret "ABCDEFGHIJKLM" :name "Red Party"   :url "http://redparty.org"}
                               {:client-id "ABCDEFGHIJKLM" :client-secret "NOPQRSTUVWXYZ" :name "Green Party" :url "http://greenparty.org"}))

(fact "will not store clients with duplicate client-ids"
      (c/store-clients-from-map client-credentials-map)
      (c/store-clients-from-map client-credentials-map)
      (cl-client/clients) => '({:client-id "NOPQRSTUVWXYZ" :client-secret "ABCDEFGHIJKLM" :name "Red Party"   :url "http://redparty.org"}
                               {:client-id "ABCDEFGHIJKLM" :client-secret "NOPQRSTUVWXYZ" :name "Green Party" :url "http://greenparty.org"}))

(tabular
  (fact "will throw exception if user credentials are invalid"
        (c/validate-client-entry {:client-id     ?client-id
                                  :name          ?name
                                  :client-secret ?client-secret
                                  :url           "random.org"}) => (throws Exception))
  ?client-id            ?name           ?client-secret
  ""                    "valid-name"    "valid-secret"
  "valid-id"            ""              "valid-secret"
  "valid-id"            "valid-name"    ""
  ""                    ""              ""
  "  "                  "valid-name"    "valid-secret"
  "\t\t"                "valid-name"    "valid-secret")

(fact "can drop the collection of clients"
      (c/store-clients-from-map client-credentials-map)
      (cl-client/clients) =not=> empty?
      (c/delete-clients!)
      (cl-client/clients) => empty?)

(fact "can retrieve client using client-id and client-secret is removed"
      (let [client-entry {:name           "name"
                          :client-id      "client-id"
                          :client-secret  "client-secret"
                          :url            "url"}]
        (cl-client/store-client client-entry)
        (c/retrieve-client "client-id") => {:name "name"
                                            :client-id "client-id"
                                            :url "url"}
        (c/retrieve-client "non-existent-client-id") => nil))

(ns stonecutter.test.db.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as cl-client]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.client :as c]
            [stonecutter.db.mongo :as m]
            [clauth.store :as cl-store]))

(def client-store (m/create-memory-store))

(background
  (before :facts (cl-store/reset-store! client-store)))

(def client-credentials
  '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "http://greenparty.org"}
    {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "http://redparty.org"}))

(fact "can store clients using credentials from the map"
      (c/store-clients-from-map client-store client-credentials)
      (cl-client/clients client-store) => '({:client-id "NOPQRSTUVWXYZ" :client-secret "ABCDEFGHIJKLM" :name "Red Party"   :url "http://redparty.org"}
                               {:client-id "ABCDEFGHIJKLM" :client-secret "NOPQRSTUVWXYZ" :name "Green Party" :url "http://greenparty.org"}))

(fact "will not store clients with duplicate client-ids"
      (c/store-clients-from-map client-store client-credentials)
      (c/store-clients-from-map client-store client-credentials)
      (cl-client/clients client-store) => '({:client-id "NOPQRSTUVWXYZ" :client-secret "ABCDEFGHIJKLM" :name "Red Party"   :url "http://redparty.org"}
                               {:client-id "ABCDEFGHIJKLM" :client-secret "NOPQRSTUVWXYZ" :name "Green Party" :url "http://greenparty.org"}))

(fact "will not store clients without url"
      (let [client-credentials-with-missing-url
            '({:client-id "ABCDEFGHIJKLL" :name "Green Party" :client-secret "NOPQRSTUVWXYZ" :url nil}
              {:client-id "NOPQRSTUVWXYL" :name "Red Party" :client-secret "ABCDEFGHIJKLM" :url "http://redparty.com"})]

        (c/store-clients-from-map client-store client-credentials-with-missing-url) => (throws Exception)))

(tabular
  (fact "will throw exception if user credentials are invalid"
        (c/validate-client-entry {:client-id     ?client-id
                                  :name          ?name
                                  :client-secret ?client-secret
                                  :url           ?url}) => (throws Exception))
  ?client-id            ?name           ?client-secret  ?url
  ""                    "valid-name"    "valid-secret"  "http://test.com"
  "valid-id"            ""              "valid-secret"  "http://test.com"
  "valid-id"            "valid-name"    ""              "http://test.com"
  "valid-id"            "valid-name"    "valid-secret"  ""
  ""                    ""              ""              "http://test.com"
  "  "                  "valid-name"    "valid-secret"  "http://test.com"
  "valid-id"            "valid-name"    "valid-secret"  "test.com"
  "\t\t"                "valid-name"    "valid-secret"  "http://test.com")

(fact "can drop the collection of clients"
      (c/store-clients-from-map client-store client-credentials)
      (cl-client/clients client-store) =not=> empty?
      (c/delete-clients! client-store)
      (cl-client/clients client-store) => empty?)

(fact "can retrieve client using client-id and client-secret is removed"
      (let [client-entry {:name           "name"
                          :client-id      "client-id"
                          :client-secret  "client-secret"
                          :url            "url"}]
        (cl-client/store-client client-store client-entry)
        (c/retrieve-client client-store "client-id") => {:name "name"
                                                                  :client-id "client-id"
                                                                  :url "url"}
        (c/retrieve-client client-store "non-existent-client-id") => nil))

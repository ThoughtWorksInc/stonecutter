(ns stonecutter.test.db.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as cl-client]
            [stonecutter.db.client :as c]
            [stonecutter.db.mongo :as m]
            [clauth.store :as cl-store]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.db.client :as client]))

(def client-store (m/create-memory-store))

(background
  (before :facts (cl-store/reset-store! client-store)))

(def client-credentials
  '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "http://greenparty.org"}
     {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "http://redparty.org"}))

(fact "can store a client with generated client-id and client-secret"
      (let [name "Red Party"
            url "http://redparty.org"]
        (c/store-client client-store name url)
        (cl-client/clients client-store) => (just (just {:client-id anything :client-secret anything :name name :url url}))))

(fact "can store clients using credentials from the map"
      (c/store-clients-from-map client-store client-credentials)
      (cl-client/clients client-store) => (just [{:client-id "NOPQRSTUVWXYZ" :client-secret "ABCDEFGHIJKLM" :name "Red Party" :url "http://redparty.org"}
                                                 {:client-id "ABCDEFGHIJKLM" :client-secret "NOPQRSTUVWXYZ" :name "Green Party" :url "http://greenparty.org"}]
                                                :in-any-order))

(fact "will not store clients with duplicate client-ids"
      (c/store-clients-from-map client-store client-credentials)
      (c/store-clients-from-map client-store client-credentials)
      (cl-client/clients client-store) => (just [{:client-id "NOPQRSTUVWXYZ" :client-secret "ABCDEFGHIJKLM" :name "Red Party" :url "http://redparty.org"}
                                                 {:client-id "ABCDEFGHIJKLM" :client-secret "NOPQRSTUVWXYZ" :name "Green Party" :url "http://greenparty.org"}]
                                                :in-any-order))

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
  ?client-id ?name ?client-secret ?url
  "" "valid-name" "valid-secret" "http://test.com"
  "valid-id" "" "valid-secret" "http://test.com"
  "valid-id" "valid-name" "" "http://test.com"
  "valid-id" "valid-name" "valid-secret" ""
  "" "" "" "http://test.com"
  "  " "valid-name" "valid-secret" "http://test.com"
  "valid-id" "valid-name" "valid-secret" "test.com"
  "\t\t" "valid-name" "valid-secret" "http://test.com")

(fact "can drop the collection of clients"
      (c/store-clients-from-map client-store client-credentials)
      (cl-client/clients client-store) =not=> empty?
      (c/delete-clients! client-store)
      (cl-client/clients client-store) => empty?)

(fact "can retrieve client using client-id and client-secret is removed"
      (let [client-entry {:name          "name"
                          :client-id     "client-id"
                          :client-secret "client-secret"
                          :url           "url"}]
        (cl-client/store-client client-store client-entry)
        (c/retrieve-client client-store "client-id") => {:name      "name"
                                                         :client-id "client-id"
                                                         :url       "url"}
        (c/retrieve-client client-store "non-existent-client-id") => nil))

(facts "about retrieving all clients"
       (fact "can retrieve all clients"
             (let [client-store (m/create-memory-store)
                   client-1 (th/store-client! client-store "name-1" "client-id-1" "client-secret-1" "client-url-1")
                   client-2 (th/store-client! client-store "name-2" "client-id-2" "client-secret-2" "client-url-2")
                   client-3 (th/store-client! client-store "name-3" "client-id-3" "client-secret-3" "client-url-3")]
               (c/retrieve-clients client-store) => (contains [client-1 client-2 client-3] :in-any-order))))

(fact "can delete a client"
      (c/store-clients-from-map client-store client-credentials)
      (c/retrieve-client client-store "ABCDEFGHIJKLM") =not=> nil
      (c/retrieve-client client-store "NOPQRSTUVWXYZ") =not=> nil
      (count (c/delete-client! client-store "ABCDEFGHIJKLM")) => 1
      (c/retrieve-client client-store "ABCDEFGHIJKLM") => nil
      (c/retrieve-client client-store "NOPQRSTUVWXYZ") =not=> nil
      (count (c/delete-client! client-store "NOPQRSTUVWXYZ")) => 0
      (c/retrieve-client client-store "NOPQRSTUVWXYZ") => nil)

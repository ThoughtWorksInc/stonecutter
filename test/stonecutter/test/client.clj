(ns stonecutter.test.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as client-store]
            [stonecutter.storage :as s]
            [stonecutter.client :refer [load-client-credentials-from-file store-clients-from-map]]))

(s/setup-in-memory-stores!)

(def client-credentials-map
  '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "nil"}
     {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "nil"}))

(facts "can load client-credentials from a file"
       (load-client-credentials-from-file "test-client-credentials.yml") => '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "nil"}
                                                                               {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "nil"}))

(facts "can store clients using credentials from the map"
       (store-clients-from-map client-credentials-map)
       (client-store/clients) => '({:client-id "NOPQRSTUVWXYZ", :client-secret "ABCDEFGHIJKLM", :name "Red Party", :url nil}
                                    {:client-id "ABCDEFGHIJKLM", :client-secret "NOPQRSTUVWXYZ", :name "Green Party", :url nil}))
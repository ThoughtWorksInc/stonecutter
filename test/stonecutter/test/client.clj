(ns stonecutter.test.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as client-store]
            [stonecutter.client :refer [load-client-credentials-from-file register-clients-from-map]]))

(def client-credentials-map
  '({:id "ABCDEFGHIJKLM", :name "Green Party", :secret "NOPQRSTUVWXYZ", :url "nil"}
     {:id "NOPQRSTUVWXYZ", :name "Red Party", :secret "ABCDEFGHIJKLM", :url "nil"}))

(facts "can load client-credentials from a file"
       (load-client-credentials-from-file "test-client-credentials.yml") => '({:id "ABCDEFGHIJKLM", :name "Green Party", :secret "NOPQRSTUVWXYZ", :url "nil"}
                                                                               {:id "NOPQRSTUVWXYZ", :name "Red Party", :secret "ABCDEFGHIJKLM", :url "nil"}))

(facts "can register clients using credentials from the map"
       (register-clients-from-map client-credentials-map)
       (client-store/clients) => (contains {:client-id "NOPQRSTUVWXYZ", :client-secret "ABCDEFGHIJKLM", :name "Red Party", :url nil}
                                           {:client-id "ABCDEFGHIJKLM", :client-secret "NOPQRSTUVWXYZ", :name "Green Party", :url nil}))
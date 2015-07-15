(ns stonecutter.test.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as cl-client]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.client :as c]))

(background
  (before :facts (storage/setup-in-memory-stores!)
          :after (storage/reset-in-memory-stores!)))

(def client-credentials-map
  '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "nil"}
    {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "nil"}))

(fact "can load client-credentials from a resource"
      (c/load-client-credentials-from-resource "test-client-credentials.yml") => '({:client-id "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z", :name "Green Resource Party", :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV", :url "nil"}
                                                                                   {:client-id "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH", :name "Red Resource Party", :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM", :url "nil"}))
(fact "can load client-credentials from a file"
      (c/load-client-credentials-from-file "test-var/var/test-client-credentials-file.yml") => '({:client-id "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D", :name "Yellow File Party", :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS", :url "nil"}
                                                                                                 {:client-id "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24", :name "Blue File Party", :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG", :url "nil"}))

(fact "can store clients using credentials from the map"
      (c/store-clients-from-map client-credentials-map)
      (cl-client/clients) => '({:client-id "NOPQRSTUVWXYZ", :client-secret "ABCDEFGHIJKLM", :name "Red Party", :url nil}
                               {:client-id "ABCDEFGHIJKLM", :client-secret "NOPQRSTUVWXYZ", :name "Green Party", :url nil}))

(fact "will not store clients with duplicate client-ids"
      (c/store-clients-from-map client-credentials-map)
      (c/store-clients-from-map client-credentials-map)
      (cl-client/clients) => '({:client-id "NOPQRSTUVWXYZ", :client-secret "ABCDEFGHIJKLM", :name "Red Party", :url nil}
                               {:client-id "ABCDEFGHIJKLM", :client-secret "NOPQRSTUVWXYZ", :name "Green Party", :url nil}))

(tabular
  (fact "will throw exception if user credentials are invalid"
        (c/validate-client-entry {:client-id     ?client-id
                                  :name          ?name
                                  :client-secret ?client-secret
                                  :url           nil}) => (throws Exception))
  ?client-id            ?name           ?client-secret
  ""                    "valid-name"    "valid-secret"
  "valid-id"            ""              "valid-secret"
  "valid-id"            "valid-name"    ""
  ""                    ""              ""
  "  "                  "valid-name"    "valid-secret"
  "\t\t"                "valid-name"    "valid-secret")

(fact "will load client credentials and store clients from a resource if provided a resource name"
      (c/load-client-credentials-and-store-clients "test-client-credentials.yml")
      (cl-client/clients) => '({:client-id "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH", :name "Red Resource Party", :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM", :url nil}
                               {:client-id "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z", :name "Green Resource Party", :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV", :url nil}))

(fact "will load and store client-credentials from a file if provided a file path"
      (c/load-client-credentials-and-store-clients "test-var/var/test-client-credentials-file.yml")
      (cl-client/clients) => '({:client-id "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24", :name "Blue File Party", :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG", :url nil}
                               {:client-id "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D", :name "Yellow File Party", :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS", :url nil}))

(fact "can drop the collection of clients"
      (c/load-client-credentials-and-store-clients "test-client-credentials.yml")
      (c/delete-clients!)
      (cl-client/clients) => [])

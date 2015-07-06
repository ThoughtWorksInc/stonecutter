(ns stonecutter.test.client
  (:require [midje.sweet :refer :all]
            [clauth.client :as client-store]
            [stonecutter.storage :as storage]
            [stonecutter.client :refer [load-client-credentials-from-resource load-client-credentials-from-file store-clients-from-map load-client-credentials]]))

(background
  (before :facts (storage/setup-in-memory-stores!)
          :after (storage/reset-in-memory-stores!)))

(def client-credentials-map
  '({:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url "nil"}
     {:client-id "NOPQRSTUVWXYZ", :name "Red Party", :client-secret "ABCDEFGHIJKLM", :url "nil"}))

(fact "can load client-credentials from a resource"
      (load-client-credentials-from-resource "test-client-credentials.yml") => '({:client-id "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z", :name "Green Resource Party", :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV", :url "nil"}
                                                                                  {:client-id "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH", :name "Red Resource Party", :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM", :url "nil"}))
(fact "can load client-credentials from a file"
      (load-client-credentials-from-file "test-var/var/test-client-credentials-file.yml") => '({:client-id "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D", :name "Yellow File Party", :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS", :url "nil"}
                                                                                                {:client-id "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24", :name "Blue File Party", :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG", :url "nil"}))

(fact "can store clients using credentials from the map"
      (store-clients-from-map client-credentials-map)
      (client-store/clients) => '({:client-id "NOPQRSTUVWXYZ", :client-secret "ABCDEFGHIJKLM", :name "Red Party", :url nil}
                                   {:client-id "ABCDEFGHIJKLM", :client-secret "NOPQRSTUVWXYZ", :name "Green Party", :url nil}))

(fact "will not store clients with duplicate client-ids"
      (store-clients-from-map client-credentials-map)
      (store-clients-from-map client-credentials-map)
      (client-store/clients) => '({:client-id "NOPQRSTUVWXYZ", :client-secret "ABCDEFGHIJKLM", :name "Red Party", :url nil}
                                   {:client-id "ABCDEFGHIJKLM", :client-secret "NOPQRSTUVWXYZ", :name "Green Party", :url nil}))

(fact "will load and store client-credentials from a resource if provided a resource name"
      (store-clients-from-map (load-client-credentials "test-client-credentials.yml"))
      (client-store/clients) => '({:client-id "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH", :name "Red Resource Party", :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM", :url nil}
                                   {:client-id "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z", :name "Green Resource Party", :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV", :url nil}))

(fact "will load and store client-credentials from a file if provided a file path"
      (store-clients-from-map (load-client-credentials "test-var/var/test-client-credentials-file.yml"))
      (client-store/clients) => '({:client-id "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24", :name "Blue File Party", :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG", :url nil}
                                   {:client-id "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D", :name "Yellow File Party", :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS", :url nil}))
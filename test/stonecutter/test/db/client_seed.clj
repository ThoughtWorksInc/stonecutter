(ns stonecutter.test.db.client-seed
  (:require [midje.sweet :refer :all]
            [clauth.client :as cl-client]
            [stonecutter.db.storage :as storage]
            [stonecutter.db.client-seed :as cs]))

(background
  (before :facts (storage/setup-in-memory-stores!)
          :after (storage/reset-in-memory-stores!)))

(fact "can load client-credentials from a resource"
      (cs/load-client-credentials-from-resource
        "test-client-credentials.yml") => '({:client-id "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z"
                                             :name "Green Resource Party"
                                             :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV"
                                             :url "http://greenresource.org"}
                                            {:client-id "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH"
                                             :name "Red Resource Party"
                                             :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM"
                                             :url "https://redresource.org"}))

(fact "can load client-credentials from a file"
      (cs/load-client-credentials-from-file
        "test-var/var/test-client-credentials-file.yml") => '({:client-id "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D"
                                                               :name "Yellow File Party"
                                                               :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS"
                                                               :url "http://yellowfile.org"}
                                                              {:client-id "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24"
                                                               :name "Blue File Party"
                                                               :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG"
                                                               :url "http://bluefile.org"}))

(fact "will load client credentials and store clients from a resource if provided a resource name"
      (cs/load-client-credentials-and-store-clients @storage/client-store "test-client-credentials.yml")
      (cl-client/clients @storage/client-store) => '({:client-id "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH"
                                :name "Red Resource Party"
                                :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM"
                                :url "https://redresource.org"}
                               {:client-id "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z"
                                :name "Green Resource Party"
                                :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV"
                                :url "http://greenresource.org"}))

(fact "will load and store client-credentials from a file if provided a file path"
      (cs/load-client-credentials-and-store-clients @storage/client-store "test-var/var/test-client-credentials-file.yml")
      (cl-client/clients @storage/client-store) => '({:client-id "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24"
                                :name "Blue File Party"
                                :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG"
                                :url "http://bluefile.org"}
                               {:client-id "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D"
                                :name "Yellow File Party"
                                :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS"
                                :url "http://yellowfile.org"}))

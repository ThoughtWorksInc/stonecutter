(ns stonecutter.test.db.client-seed
  (:require [midje.sweet :refer :all]
            [clauth.client :as cl-client]
            [stonecutter.db.client-seed :as cs]
            [stonecutter.db.mongo :as m]
            [clauth.store :as cl-store]))

(def client-store (m/create-memory-store))

(background
  (before :facts (cl-store/reset-store! client-store)))

(fact "can load client-credentials from a resource"
      (cs/load-client-credentials-from-resource
        "test-client-credentials.yml") => (just [{:client-id     "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z"
                                                  :name          "Green Resource Party"
                                                  :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV"
                                                  :url           "http://greenresource.org"}
                                                 {:client-id     "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH"
                                                  :name          "Red Resource Party"
                                                  :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM"
                                                  :url           "https://redresource.org"}]
                                                :in-any-order))

(fact "can load client-credentials from a file"
      (cs/load-client-credentials-from-file
        "test-var/var/test-client-credentials-file.yml") => (just [{:client-id     "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D"
                                                                    :name          "Yellow File Party"
                                                                    :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS"
                                                                    :url           "http://yellowfile.org"}
                                                                   {:client-id     "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24"
                                                                    :name          "Blue File Party"
                                                                    :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG"
                                                                    :url           "http://bluefile.org"}]
                                                                  :in-any-order))

(fact "will load client credentials and store clients from a resource if provided a resource name"
      (cs/load-client-credentials-and-store-clients client-store "test-client-credentials.yml")
      (cl-client/clients client-store) => (just [{:client-id     "M4DPY7IO5KZ77KRHMXYTACUZEEZEK4FH"
                                                  :name          "Red Resource Party"
                                                  :client-secret "VZ27BQ5GWLWXVFQQBPGDIPY4QTDEUZNM"
                                                  :url           "https://redresource.org"}
                                                 {:client-id     "ZW76L2MGCVMQBN44VYRE7MS5JOZMUE2Z"
                                                  :name          "Green Resource Party"
                                                  :client-secret "WUSB7HHQIYEZNIGZ4HT4BSHEEAYCCKV"
                                                  :url           "http://greenresource.org"}]
                                                :in-any-order))

(fact "will load and store client-credentials from a file if provided a file path"
      (cs/load-client-credentials-and-store-clients client-store "test-var/var/test-client-credentials-file.yml")
      (cl-client/clients client-store) => (just [{:client-id     "F7ZFMTRJLS5FF6DTWMBCGWA7CXEQTH24"
                                                  :name          "Blue File Party"
                                                  :client-secret "ISLS5HVYP65QE6OROI43FLHFFOBGISOG"
                                                  :url           "http://bluefile.org"}
                                                 {:client-id     "7I6DTMPXGESEJ2AEEWVGZF2B5AOFGK6D"
                                                  :name          "Yellow File Party"
                                                  :client-secret "ABH7ZKGVMKQQA3OITBOXPBVSZMMD5NGS"
                                                  :url           "http://yellowfile.org"}]
                                                :in-any-order))

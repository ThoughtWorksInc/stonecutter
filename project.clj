(defproject stonecutter "0.1.0-SNAPSHOT"
            :description "A D-CENT project: an easily deployable oauth server for small organisations."
            :url "https://stonecutter.herokuapp.com"
            :min-lein-version "2.0.0"
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [ring/ring-defaults "0.1.2"]
                           [ring/ring-jetty-adapter "1.4.0-RC1"]
                           [scenic "0.2.3"]
                           [enlive "1.1.5"]
                           [hiccup "1.0.5"]
                           [johncowie/clauth "1.0.1-SNAPSHOT"]
                           [traduki "0.1.1-SNAPSHOT"]
                           [clj-yaml "0.4.0"]
                           [environ "1.0.0"]
                           [com.novemberain/monger "2.0.0"]
                           [org.clojure/tools.logging "0.3.1"]
                           [clj-logging-config "1.9.12"]
                           [crypto-random "1.1.0"]
                           [prismatic/schema "0.4.3"]]
            :main stonecutter.handler
            :aot :all
            :profiles {:dev {:dependencies   [[ring-mock "0.1.5"]
                                              [midje "1.6.3"]
                                              [prone "0.8.2"]
                                              [kerodon "0.6.1"]]
                             :plugins        [[lein-ring "0.9.6"]
                                              [lein-midje "3.1.3"]]
                             :ring {:handler stonecutter.handler/lein-app
                                    :init    stonecutter.handler/lein-ring-init
                                    :stacktrace-middleware prone.middleware/wrap-exceptions}
                             :resource-paths ["resources" "test-resources"]
                             :aliases        {"unit"        ["midje" "stonecutter.test.*"]
                                              "integration" ["midje" "stonecutter.integration.*"]
                                              "auto-unit"   ["midje" ":autotest" "test/stonecutter/test/" "src/"]
                                              "gencred"     ["run" "-m" "stonecutter.util.gencred"]}}})

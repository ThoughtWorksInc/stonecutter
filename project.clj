(defproject stonecutter "0.1.0-SNAPSHOT"
            :description "A D-CENT project: an easily deployable oauth server for small organisations."
            :url "https://stonecutter.herokuapp.com"
            :min-lein-version "2.0.0"
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [ring/ring-defaults "0.1.5"]
                           [ring/ring-jetty-adapter "1.4.0"]
                           [scenic "0.2.3"]
                           [enlive "1.1.6"]
                           [hiccup "1.0.5"]
                           [com.cemerick/url "0.1.1"]
                           [johncowie/clauth "1.0.1"]
                           [traduki "0.1.1-SNAPSHOT"]
                           [clj-yaml "0.4.0"]
                           [environ "1.0.0"]
                           [com.novemberain/monger "2.0.0"]
                           [org.clojure/tools.logging "0.3.1"]
                           [clj-logging-config "1.9.12"]
                           [crypto-random "1.2.0"]
                           [prismatic/schema "0.4.3"]
                           [ragtime "0.5.0"]]
            :main stonecutter.handler
            :aot :all
            :profiles {:dev {:dependencies   [[ring-mock "0.1.5"]
                                              [midje "1.6.3"]
                                              [prone "0.8.2"]
                                              [kerodon "0.6.1"]]
                             :plugins        [[lein-ring "0.9.6"]
                                              [lein-environ "1.0.0"]
                                              [lein-midje "3.1.3"]
                                              [lein-kibit "0.1.2"]
                                              [lein-ancient "0.6.7"]]
                             :ring {:handler stonecutter.handler/lein-app
                                    :init    stonecutter.handler/lein-ring-init
                                    :stacktrace-middleware prone.middleware/wrap-exceptions}
                             :resource-paths ["resources" "test-resources"]
                             :aliases        {"test"        "midje"
                                              "unit"        ["midje" "stonecutter.test.*"]
                                              "integration" ["midje" "stonecutter.integration.*"]
                                              "auto-unit"   ["midje" ":autotest" "test/stonecutter/test/" "src/"]
                                              "gencred"     ["run" "-m" "stonecutter.util.gencred"]
                                              "lint"        ["eastwood" "{:namespaces [:source-paths]}"]}
                             :env {:secure "false"
                                   :email-script-path "test-resources/mail_stub.sh"}}})

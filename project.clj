(defproject stonecutter "0.1.0-SNAPSHOT"
  :description "A D-CENT project: an easily deployable oauth server for small organisations."
  :url "https://stonecutter.herokuapp.com"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [scenic "0.2.3"]
                 [enlive "1.1.6"]
                 [hiccup "1.0.5"]
                 [com.cemerick/url "0.1.1"]
                 [johncowie/clauth "2.0.1"]
                 [traduki "0.1.3-SNAPSHOT"]
                 [clj-yaml "0.4.0"]
                 [environ "1.0.0"]
                 [com.novemberain/monger "2.1.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-logging-config "1.9.12"]
                 [crypto-random "1.2.0"]
                 [prismatic/schema "0.4.3"]
                 [ragtime "0.4.2"]
                 [garden "1.2.5"]
                 [clj-time "0.10.0"]
                 [org.bitbucket.b_c/jose4j "0.4.4"]
                 [org.slf4j/slf4j-simple "1.7.12"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]]
  :main stonecutter.handler
  :jvm-opts ["-Dlog4j.configuration=log4j.dev"]
  :aot :all
  :profiles {:dev     {:dependencies   [[ring-mock "0.1.5"]
                                        [midje "1.7.0"]
                                        [prone "0.8.2"]
                                        [kerodon "0.6.1"]]
                       :plugins        [[lein-ring "0.9.6"]
                                        [lein-environ "1.0.0"]
                                        [lein-midje "3.1.3"]
                                        [lein-kibit "0.1.2"]
                                        [lein-ancient "0.6.7"]
                                        [lein-cljsbuild "1.0.6"]
                                        [com.cemerick/clojurescript.test "0.3.3"]]
                       :ring           {:reload-paths          ["src"]
                                        :handler               stonecutter.lein/lein-app
                                        :init                  stonecutter.lein/lein-ring-init
                                        :stacktrace-middleware prone.middleware/wrap-exceptions}
                       :resource-paths ["resources" "test-resources"]
                       :aliases        {"test"        ["do" "clean," "midje," "test-cljs"]
                                        "test-cljs"   ["cljsbuild" "test"]
                                        "unit"        ["midje" "stonecutter.test.*"]
                                        "integration" ["midje" "stonecutter.integration.*"]
                                        "auto-unit"   ["midje" ":autotest" "test/stonecutter/test/" "src/"]
                                        "gencred"     ["run" "-m" "stonecutter.util.gencred"]
                                        "gen-keypair" ["run" "-m" "stonecutter.util.gen-key-pair"]
                                        "lint"        ["eastwood" "{:namespaces [:source-paths]}"]}
                       :env            {:dev                   true
                                        :secure                "false"
                                        :rsa-keypair-file-path "test-resources/test-key.json"}
                       :cljsbuild      {:builds        [{:source-paths ["src-cljs"]
                                                         :compiler     {:output-to  "resources/public/js/change_password.js"
                                                                        :output-dir "resources/public/js/out"
                                                                        :main       "stonecutter.change-password"
                                                                        :asset-path "js/out"
                                                                        :optimizations :whitespace
                                                                        :pretty-print  true
                                                                        :source-map    true}}
                                                        {:source-paths ["src-cljs" "test-cljs"]
                                                         :compiler     {:output-to     "target/cljs/testable.js"
                                                                        :optimizations :whitespace}}]
                                        :test-commands {"unit-tests" ["phantomjs" :runner
                                                                      "window.literal_js_was_evaluated=true"
                                                                      "target/cljs/testable.js"]}}}


             :uberjar {:hooks       [leiningen.cljsbuild]
                       :env         {:production true}
                       :aot         :all
                       :omit-source true
                       :cljsbuild   {:jar    true
                                     :builds [{:source-paths ["src-cljs"]
                                               :output-to    "resources/public/js/change_password.js"
                                               :compiler     {:optimizations :advanced
                                                              :pretty-print  false}}]}}}
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]
  )

(defproject stonecutter "0.1.0-SNAPSHOT" ;test for story #317
  :description "A D-CENT project: an easily deployable oauth server for small organisations."
  :url "https://stonecutter.herokuapp.com"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [clj-http "2.0.0"]
                 [scenic "0.2.5"]
                 [enlive "1.1.6"]
                 [hiccup "1.0.5"]
                 [hickory "0.5.4"]
                 [com.cemerick/url "0.1.1"]
                 [johncowie/clauth "2.0.1"]
                 [com.taoensso/tower "3.0.2"]
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
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [prismatic/dommy "1.1.0"]
                 [cljs-ajax "0.3.14"]
                 [ring/ring-json "0.4.0"]
                 [commons-codec/commons-codec "1.10"]
                 [commons-io/commons-io "2.4"]
                 [image-resizer "0.1.8"]
                 [com.novemberain/pantomime "2.8.0"]]
  :main stonecutter.handler
  :jvm-opts ["-Dlog4j.configuration=log4j.dev"]
  :aot :all
  :source-paths ["src" "src-cljc"]
  :profiles {:dev     {:dependencies   [[ring-mock "0.1.5"]
                                        [midje "1.7.0"]
                                        [prone "0.8.2"]
                                        [clj-webdriver "0.7.2" :exclusions [org.seleniumhq.selenium/selenium-java
                                                                            org.seleniumhq.selenium/selenium-server
                                                                            org.seleniumhq.selenium/selenium-remote-driver
                                                                            xml-apis]]
                                        [xml-apis "1.4.01"]
                                        [org.seleniumhq.selenium/selenium-server "2.45.0"]
                                        [org.seleniumhq.selenium/selenium-java "2.45.0"]
                                        [org.seleniumhq.selenium/selenium-remote-driver "2.45.0"]
                                        [kerodon "0.6.1"]]
                       :plugins        [[lein-ring "0.9.6"]
                                        [lein-auto "0.1.2"]
                                        [lein-environ "1.0.0"]
                                        [lein-midje "3.1.3"]
                                        [lein-kibit "0.1.2"]
                                        [lein-ancient "0.6.7"]
                                        [lein-cljsbuild "1.0.6"]
                                        [lein-shell "0.4.1"]
                                        [com.cemerick/clojurescript.test "0.3.3"]]
                       :ring           {:reload-paths          ["src" "src-cljc"]
                                        :handler               stonecutter.lein/lein-app
                                        :init                  stonecutter.lein/lein-ring-init
                                        :stacktrace-middleware prone.middleware/wrap-exceptions}
                       :resource-paths ["resources" "test-resources"]
                       :aliases        {"cljs-build"      ["cljsbuild" "once" "prod"]
                                        "test"            ["do" "test-cljs," "test-clj"]
                                        "test-clj"        ["shell" "test/stonecutter/run_all_tests.sh"]
                                        "unit"            ["do" "gulp," "midje" "stonecutter.test.*"]
                                        "integration"     ["do" "gulp," "midje" "stonecutter.integration.*"]
                                        "browser"         ["shell" "test/stonecutter/run_browser_tests.sh"]
                                        "auto-no-browser" ["test-clj" ":autotest" "src/" "src-cljc/"
                                                           "test/stonecutter/test/" "test/stonecutter/integration/"]
                                        "test-cljs"       ["do" "clean," "gulp," "cljsbuild" "test"]
                                        "auto-cljs"       ["auto" "cljsbuild" "test"]
                                        "gencred"         ["run" "-m" "stonecutter.util.gencred"]
                                        "gen-keypair"     ["run" "-m" "stonecutter.util.gen-key-pair"]
                                        "gen-config"      ["run" "-m" "stonecutter.config"]
                                        "lint"            ["eastwood" "{:namespaces [:source-paths]}"]
                                        "gulp"            ["shell" "npm" "run" "gulp" "--" "build"]
                                        "start"           ["do" "gulp," "cljs-build," "run"]}
                       :auto           {"cljsbuild" {:paths ["src-cljs" "src-cljc" "test-cljs"]}}
                       :env            {:dev                   true
                                        :secure                "false"
                                        :rsa-keypair-file-path "test-resources/test-key.json"
                                        :admin-login           "dcent@thoughtworks.com"
                                        :admin-password        "password"
                                        :admin-first-name      "first"
                                        :admin-last-name       "last"
                                        :host                  "0.0.0.0"
                                        :port                  "5000"
                                        :profile-image-path    "resources/public"}
                       :cljsbuild      {:builds        [{:id           "prod"
                                                         :source-paths ["src-cljs" "src-cljc"]
                                                         :compiler     {:output-to     "resources/public/js/main.js"
                                                                        :asset-path    "js/out"
                                                                        :optimizations :advanced
                                                                        :pretty-print  false}}
                                                        {:id           "test"
                                                         :source-paths ["src-cljs" "src-cljc" "test-cljs"]
                                                         :compiler     {:output-to     "target/cljs/testable.js"
                                                                        :optimizations :whitespace
                                                                        :pretty-print  true}}]
                                        :test-commands {"phantom" ["phantomjs" :runner "target/cljs/testable.js"]}}}
             :uberjar {:prep-tasks  ["compile" ["cljsbuild" "once"]]
                       :env         {:production true}
                       :aot         :all
                       :omit-source true
                       :cljsbuild   {:builds [{:source-paths ["src-cljs" "src-cljc"]
                                               :compiler     {:output-to     "resources/public/js/main.js"
                                                              :optimizations :advanced
                                                              :pretty-print  false}}]}}
             :docker  {:env {:host         "0.0.0.0"
                             :port         "5000"
                             :mongodb-port "27017"
                             :mongodb-db   "coracle"}}}
  :cljsbuild   {:builds [{:source-paths ["src-cljs" "src-cljc"]
                          :compiler     {:output-to     "resources/public/js/main.js"
                                         :optimizations :advanced
                                         :pretty-print  false}}]}
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]])

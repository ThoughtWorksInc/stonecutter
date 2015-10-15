(ns stonecutter.browser.journey-test
  (:require [midje.sweet :refer :all]
            [clj-webdriver.taxi :as wd]
            [clojure.test :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-webdriver.taxi :refer :all]
            [stonecutter.handler :as h]
            [stonecutter.config :as config]
            [stonecutter.db.storage :as storage]
            [stonecutter.admin :as ad]
            [stonecutter.browser.common :as c]))

(def test-port 5439)

(defn start-server []
  (let [stores-m (storage/create-in-memory-stores)
        config-m (config/create-config)
        app-routes (h/create-app config-m {} stores-m {} {} {})
        _admin (ad/create-admin-user {:admin-login "test@test.com" :admin-password "password"} (storage/get-user-store stores-m))]
    (loop [server (run-jetty app-routes {:port test-port :host "localhost" :join? false})]
      (if (.isStarted server)
        server
        (recur server)))))

(defn stop-server [server]
  (.stop server))

(defn start-browser []
  (wd/set-driver! {:browser :firefox}))

(defn stop-browser []
  (wd/quit))

(def server (atom {}))

(against-background
  [(before :contents (do (reset! server (start-server))
                         (start-browser)))
   (after :contents (do
                      (stop-browser)
                        (stop-server @server)))]

  (try
    (facts "admin journey on user-list page" :browser
          (wd/to c/localhost)
          (c/wait-for-selector c/stonecutter-index-page-body)

          (c/input-register-credentials-and-submit)

          (wd/to (str c/localhost "/sign-out"))
          (c/wait-for-selector c/stonecutter-index-page-body)
          (wd/current-url) => (contains (str c/localhost "/"))

          (wd/input-text c/stonecutter-sign-in-email-input "test@test.com")
          (wd/input-text c/stonecutter-sign-in-password-input "password")
          (wd/click c/stonecutter-sign-in-button)
          (wd/current-url) => (contains "/profile")

           (fact "admin can toggle users"
                 (wd/to c/user-list-page)

                 (wd/attribute c/stonecutter-trust-toggle :checked) => nil
                 (wd/toggle c/stonecutter-trust-toggle)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => "checked"

                 (wd/to c/user-list-page)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => "checked"

                 (wd/toggle c/stonecutter-trust-toggle)
                 (wd/to c/user-list-page)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => nil))
    (catch Exception e
      (throw e))))
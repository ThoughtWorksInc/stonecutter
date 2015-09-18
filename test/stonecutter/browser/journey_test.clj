(ns stonecutter.browser.journey-test
  (:require [midje.sweet :refer :all]
            [clj-webdriver.taxi :as wd]
            [clojure.test :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-webdriver.taxi :refer :all]
            [stonecutter.handler :as h]
            [stonecutter.config :as config]
            [stonecutter.db.storage :as storage]
            [stonecutter.admin :as ad]))

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

;; COMMON
(def stonecutter-index-page-body ".func--index-page")
(def stonecutter-sign-in-email-input ".func--sign-in-email__input")
(def stonecutter-sign-in-password-input ".func--sign-in-password__input")
(def stonecutter-sign-in-button ".func--sign-in__button")

(def stonecutter-register-first-name-input ".func--registration-first-name__input")
(def stonecutter-register-last-name-input ".func--registration-last-name__input")
(def stonecutter-register-email-input ".func--registration-email__input")
(def stonecutter-register-password-input ".func--registration-password__input")
(def stonecutter-register-create-profile-button ".func--create-profile__button")

(def stonecutter-trust-toggle ".clj--user-item__toggle")

(defn input-register-credentials-and-submit []
  (wd/input-text stonecutter-register-first-name-input "Journey")
  (wd/input-text stonecutter-register-last-name-input "Test")
  (wd/input-text stonecutter-register-email-input "stonecutter-journey-test@tw.com")
  (wd/input-text stonecutter-register-password-input "password")
  (wd/click stonecutter-register-create-profile-button))


(defn wait-for-selector [selector]
  (try
    (wd/wait-until #(not (empty? (wd/css-finder selector))) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>> Selector could not be found: " selector))
      (prn "==========  PAGE SOURCE ==========")
      (prn (wd/page-source))
      (prn "==========  END PAGE SOURCE ==========")
      (throw e))))

;;______________

(def localhost "localhost:5439")

(against-background
  [(before :contents (do (reset! server (start-server))
                         (start-browser)))
   (after :contents (do (stop-browser)
                        (stop-server @server)))]

  (try
    (fact "can login as admin account" :browser
          (wd/to localhost)
          (wait-for-selector stonecutter-index-page-body)

          (input-register-credentials-and-submit)

          (wd/to (str localhost "/sign-out"))
          (wait-for-selector stonecutter-index-page-body)
          (wd/current-url) => (contains (str localhost "/"))

          (wd/input-text stonecutter-sign-in-email-input "test@test.com")
          (wd/input-text stonecutter-sign-in-password-input "password")
          (wd/click stonecutter-sign-in-button)
          (wd/current-url) => (contains "/profile")

          (wd/to (str localhost "/admin/users"))

          (wd/attribute stonecutter-trust-toggle :checked) => nil
          (wd/toggle stonecutter-trust-toggle)
          (wd/attribute stonecutter-trust-toggle :checked) => "checked"

          (wd/to (str localhost "/admin/users"))
          (wd/attribute stonecutter-trust-toggle :checked) => "checked"

          (wd/toggle stonecutter-trust-toggle)
          (wd/to (str localhost "/admin/users"))
          (wd/attribute stonecutter-trust-toggle :checked) => nil

          )


    (catch Exception e
      (prn "hello, you have an error"))))

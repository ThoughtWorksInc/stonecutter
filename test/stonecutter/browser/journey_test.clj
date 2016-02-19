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
            [stonecutter.browser.common :as c]
            [stonecutter.db.invitations :as i]
            [stonecutter.test.util.time :as test-time]
            [stonecutter.email :as email]
            [monger.core :as monger]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(def test-port 5439)

(def invited-email "somewhere@somewhere.com")
(def invited-id "10")
(def id-generation-fn (constantly invited-id))

(def conn (atom nil))

(defn start-server []
  (reset! conn (monger/connect))
  (let [stores-m (storage/create-in-memory-stores @conn)
        config-m (config/create-config)
        app-routes (h/create-app config-m {} stores-m (email/create-stdout-email-sender) {} {})
        _admin (ad/create-admin-user {:admin-login "test@test.com" :admin-password "password"} (storage/get-user-store stores-m))]
    (i/generate-invite-id! (:invitation-store stores-m) invited-email (test-time/new-stub-clock 0) 7 id-generation-fn)
    (loop [server (run-jetty app-routes {:port test-port :host "localhost" :join? false})]
      (if (.isStarted server)
        server
        (recur server)))))

(defn stop-server [server]
  (.stop server)
  (monger/disconnect @conn))

(defn start-browser []
  (wd/set-driver! {:browser :firefox}))

(defn stop-browser []
  (wd/quit))

(def screenshot-directory "test/stonecutter/browser/screenshots")
(def screenshot-number (atom 0))
(defn screenshot [filename]
  (log/info (str "Screenshot: " filename))
  (wd/take-screenshot :file (str screenshot-directory "/"
                                 (format "%02d" (swap! screenshot-number + 1))
                                 "_" filename ".png")))

(defn clear-screenshots []
  (doall (->> (io/file screenshot-directory)
              file-seq
              (filter #(re-matches #".*\.png$" (.getName %)))
              (map io/delete-file))))


(def server (atom {}))

(against-background
  [(before :contents (do (reset! server (start-server))
                         (clear-screenshots)
                         (start-browser)))
   (after :contents (do
                      (stop-browser)
                      (stop-server @server)))]

  (try
    (facts "Invite registration and user list journey" :browser
           (wd/to (c/accept-invite invited-id))
           (c/wait-for-selector c/stonecutter-accept-invite-page-body)
           (screenshot "sign_up_page")
           (c/input-register-credentials-and-submit)
           (screenshot "signed-up-confirmation")
           (wd/to (str c/localhost "/sign-out"))

           (c/wait-for-selector c/stonecutter-index-page-body)
           (wd/current-url) => (contains (str c/localhost "/"))
           (screenshot "index_page")

           (wd/input-text c/stonecutter-sign-in-email-input "test@test.com")
           (wd/input-text c/stonecutter-sign-in-password-input "password")
           (wd/click c/stonecutter-sign-in-button)
           (wd/current-url) => (contains "/profile")
           (screenshot "profile_page")

           (fact "invited user is automatically trusted"
                 (wd/to c/user-list-page)
                 (screenshot "user list")
                 (c/wait-for-selector c/stonecutter-user-list-page-body)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => "checked")

           (fact "admin can toggle users"
                 (wd/to c/user-list-page)
                 (screenshot "user_list_user_toggled")
                 (c/wait-for-selector c/stonecutter-user-list-page-body)

                 (wd/attribute c/stonecutter-trust-toggle :checked) => "checked"
                 (wd/toggle c/stonecutter-trust-toggle)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => nil

                 (wd/to c/user-list-page)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => nil

                 (wd/toggle c/stonecutter-trust-toggle)
                 (wd/to c/user-list-page)
                 (wd/attribute c/stonecutter-trust-toggle :checked) => "checked"))
    (catch Exception e
      (throw e))))


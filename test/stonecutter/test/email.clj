(ns stonecutter.test.email
  (:require [midje.sweet :refer :all]
            [stonecutter.email :as email]))

(def test-state (atom nil))

(defn reset-test-state! []
  (reset! test-state nil))

(defprotocol EmailRecorder
  (last-sent-email [this])
  (reset-emails! [this]))

(defrecord TestEmailSender [email-store]
  email/EmailSender
  (send-email! [this email-address subject body]
    (swap! email-store (partial cons {:email email-address :subject subject :body body})))
  EmailRecorder
  (last-sent-email [this]
    (first @email-store))
  (reset-emails! [this]
    (reset! email-store [])))

(defn create-test-email-sender []
 (TestEmailSender. (atom [])))

(defn test-email-renderer [subject body]
  (constantly
    {:subject subject :body body}))

(fact "renders and sends an email generated using the specified template"
      (let [email-sender (create-test-email-sender)]
        (email/initialise! {:template-label (test-email-renderer ...subject... ...body...)})
        (email/send! email-sender :template-label ...email-address... ...email-data-map...)
        (let [sent-email (last-sent-email email-sender)]
          (:email sent-email) => ...email-address...
          (:subject sent-email) => ...subject...
          (:body sent-email) => ...body...)))

(fact "testing the forgotten password renderer"
      (let [app-name "MyTestApp"
            base-url "base-url"
            email-data {:app-name app-name :base-url base-url :forgotten-password-id "forgotten-password-id"}
            email (email/forgotten-password-renderer email-data)]

        (:subject email) => (str "Reset password for " app-name)
        (:body email) => (str base-url "/reset-password/forgotten-password-id")))

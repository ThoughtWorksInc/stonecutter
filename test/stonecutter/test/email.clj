(ns stonecutter.test.email
  (:require [midje.sweet :refer :all]
            [stonecutter.email :as email]))

(def test-state (atom nil))

(defn reset-test-state! []
  (reset! test-state nil))

(defn test-email-sender! [email subject body]
  (reset! test-state {:email email
                      :subject subject
                      :body body}))

(defn test-email-renderer-factory [subject body]
  (constantly
    {:subject subject
     :body body}))

(fact "renders and sends an email generated using the specified template"
      (reset-test-state!)
      (email/initialise! test-email-sender! {:template-label (test-email-renderer-factory ...subject... ...body...)})
      (email/send! :template-label ...email-address... ...email-data-map...)

      (:email @test-state) => ...email-address...
      (:subject @test-state) => ...subject...
      (:body @test-state) => ...body...)

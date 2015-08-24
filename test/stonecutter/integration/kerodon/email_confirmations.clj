(ns stonecutter.integration.kerodon.email-confirmations
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clojure.java.io :as io]
            [stonecutter.email :as email]
            [stonecutter.integration.kerodon.kerodon-checkers :as kh]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.routes :as routes]
            [stonecutter.handler :as h]
            [stonecutter.logging :as l]
            [stonecutter.db.storage :as storage]
            [stonecutter.integration.kerodon.steps :as steps]))

(l/init-logger!)

(defn parse-test-email []
  (read-string (slurp "test-tmp/test-email.txt")))

(defn checks-email-is-sent [state email-address]
  (fact {:midje/name "Check send email script is called"}
        (parse-test-email) => (contains {:email-address email-address}))
  state)

(defn delete-directory [directory-path]
  (->> (io/file directory-path)
       file-seq
       reverse
       (map io/delete-file)
       doall))

(defn setup-test-directory [state]
  (fact {:midje/name "setup test tmp directory"}
        (io/make-parents "test-tmp/dummy.txt")
        (.exists (io/file "test-tmp")) => true)
  state)

(defn teardown-test-directory [state]
  (fact {:midje/name "teardown test tmp directory"}
        (delete-directory "test-tmp")
        (.exists (io/file "test-tmp")) => false)
  state)

(defn test-email-renderer [email-data]
  {:subject ""
   :body    (str email-data)})

(background (email/get-confirmation-renderer) => test-email-renderer)

(def email-sender (email/bash-sender-factory "test-resources/mail_stub.sh"))

(def test-app (ih/build-app {:email-sender email-sender}))

(facts "User is not confirmed when first registering for an account; Hitting the confirmation endpoint confirms the user account when the UUID in the uri matches that for the signed in user's account"
       (-> (k/session test-app)

           (setup-test-directory)

           (steps/register "confirmation-test@email.com" "valid-password")

           (k/visit (routes/path :show-profile))
           (kh/selector-exists [:.clj--email-not-confirmed-message])
           (kh/selector-not-present [:.clj--email-confirmed-message])

           (k/visit (routes/path :confirm-email-with-id
                                 :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))
           (kh/check-and-follow-redirect)
           (kh/page-uri-is (routes/path :home))
           (k/follow-redirect)
           (kh/page-uri-is (routes/path :show-profile))
           (kh/selector-not-present [:.clj--email-not-confirmed-message])
           (kh/selector-exists [:.clj--email-confirmed-message])

           (teardown-test-directory)))

(facts "The account confirmation flow can be followed by a user who is not signed in when first accessing the confirmation endpoint"
       (-> (k/session test-app)

           (setup-test-directory)

           (steps/register "confirmation-test-2@email.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/sign-out-link)
           (kh/check-and-follow-redirect "just signed out")

           (k/visit (routes/path :confirm-email-with-id
                                 :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))
           (kh/page-uri-is (routes/path :confirm-email-with-id
                                        :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))

           (kh/check-and-follow-redirect "redirecting to sign in")
           (kh/page-uri-is (routes/path :confirmation-sign-in-form
                                        :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))
           (k/fill-in ks/sign-in-password-input "valid-password")
           (k/press ks/sign-in-submit)

           (k/follow-redirect)
           (kh/page-uri-is (routes/path :confirm-email-with-id
                                        :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))

           (k/follow-redirect)
           (kh/page-uri-is (routes/path :home))
           (k/follow-redirect)
           (kh/page-uri-is (routes/path :show-profile))
           (kh/selector-not-present [:.clj--email-not-confirmed-message])
           (kh/selector-exists [:.clj--email-confirmed-message])

           (teardown-test-directory)))

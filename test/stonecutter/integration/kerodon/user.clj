(ns stonecutter.integration.kerodon.user
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clauth.client :as cl-client]
            [clojure.java.io :as io]
            [stonecutter.email :as email]
            [stonecutter.routes :as routes]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.db.user :as user]
            [stonecutter.view.register :as register-view]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.kerodon.kerodon-checkers :as kh]
            [stonecutter.integration.kerodon.steps :as steps]
            ))

(l/init-logger!)
(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))

(defn setup-add-client-to-user! [email client-name]
  (let [client (cl-client/register-client (s/get-client-store stores-m) client-name "myclient.com")
        client-id (:client-id client)]
    (user/add-authorised-client-for-user! (s/get-user-store stores-m) email client-id)))

(defn parse-test-email []
  (read-string (slurp "test-tmp/test-email.txt")))

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
   :body (str email-data)})

(def email-sender (email/bash-sender-factory "test-resources/mail_stub.sh"))

(def test-app (ih/build-app {:stores-m stores-m :email-sender email-sender}))

(facts "Home url redirects to sign-in page if user is not signed in"
       (-> (k/session test-app)
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/sign-in-page-body])))

(facts "User is returned to same page when email is invalid"
       (-> (k/session test-app)
           (k/visit "/register")
           (k/fill-in "Email address" "invalid-email")
           (k/press ks/registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/registration-page-body])
           (kh/selector-includes-content [ks/registration-email-validation-element] "Enter a valid email address")))

(facts "User is returned to same page when existing email is used"
       (-> (k/session test-app)
           (steps/register "existing@user.com" "password")
           (k/visit "/register")
           (k/fill-in ks/registration-email-input "existing@user.com")
           (k/fill-in ks/registration-password-input "password")
           (k/fill-in ks/registration-confirm-input "password")
           (k/press ks/registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/registration-page-body])))

(facts "Register page redirects to profile-created page when registered and
       user-login is in the session so that email address is displayed on profile card"
       (-> (k/session test-app)
           (steps/register "email@server.com" "valid-password")
           (k/follow-redirect)
           (kh/page-uri-is "/profile-created")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-created-page-body])
           (kh/selector-includes-content [ks/profile-created-flash] "email@server.com")

           (k/visit "/profile")
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-page-body])
           (kh/selector-includes-content [:.func--card-email] "email@server.com")))

(facts "User is redirected to sign-in page when accessing profile page not signed in"
       (-> (k/session test-app)
           (k/visit "/profile")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/sign-in-page-body])))

(facts "User can sign in"
       (-> (k/session test-app)
           (steps/sign-in "email@server.com" "valid-password")
           (k/follow-redirect)
           (kh/page-uri-is "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-page-body])
           (kh/selector-includes-content [:body] "email@server.com")))

(facts "User can sign out"
       (-> (k/session test-app)
           (steps/sign-in "email@server.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/sign-out-link)
           (k/follow-redirect)
           (kh/page-uri-is "/")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")))

(facts "Home url redirects to profile page if user is signed in"
       (-> (k/session test-app)
           (steps/sign-in "email@server.com" "valid-password")
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "Home url redirects to profile page if user is registered"
       (-> (k/session test-app)
           (steps/register "email2@server.com" "valid-password")
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "Clients appear on user profile page"
       (-> (k/session test-app)
           (steps/register "user@withclient.com" "valid-password"))
       (setup-add-client-to-user! "user@withclient.com" "myapp")
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (kh/selector-includes-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can unshare profile card"
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (kh/selector-includes-content [ks/profile-authorised-client-list] "myapp")
           (k/follow ks/profile-authorised-client-unshare-link)
           (kh/page-uri-contains "/unshare-profile-card")
           (k/press ks/unshare-profile-card-confirm-button)
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/selector-does-not-include-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can change password"
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/profile-change-password-link)
           (kh/page-uri-is "/change-password")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/change-password-page-body])
           (k/fill-in ks/change-password-current-password-input "valid-password")
           (k/fill-in ks/change-password-new-password-input "new-valid-password")
           (k/fill-in ks/change-password-confirm-new-password-input "new-valid-password")
           (k/press ks/change-password-submit)
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-page-body])
           (kh/selector-exists [ks/profile-flash-message])))

(facts "User can delete account"
       (-> (k/session test-app)
           (steps/register "account_to_be@deleted.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/profile-delete-account-link)
           (kh/page-uri-is "/delete-account")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/delete-account-page-body])
           (k/press ks/delete-account-button)
           (k/follow-redirect)
           (kh/page-uri-is "/profile-deleted")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-deleted-page-body])))

(facts "Not found page is shown for unknown url"
       (-> (k/session test-app)
           (k/visit "/wrong-url")
           (kh/response-status-is 404)
           (kh/selector-exists [ks/error-404-page-body])))

(fact "Error page is shown if an exception is thrown"
      (against-background
        (register-view/registration-form anything) =throws=> (Exception.))
      (-> (k/session (ih/build-app {:prone-stack-tracing? false}))
          (k/visit "/register")
          (kh/response-status-is 500)
          (kh/selector-exists [ks/error-500-page-body]))
      (fact "if prone stack-tracing is enabled then error middleware isn't invoked (exception not caught)"
            (-> (k/session (ih/build-app {:prone-stack-tracing? true}))
                (k/visit "/register")) => (throws Exception)))

(fact "theme.css file is generated using environment variables"
      (-> (k/session (ih/build-app {:config-m {:secure "false"
                                               :header-bg-color "#012345"
                                               :header-font-color "#ABCDEF"
                                               :header-font-color-hover "#FEDCBA"
                                               :static-resources-dir-path "./test-resources"
                                               :logo-file-name "beautiful_logo.png"}}))
          (k/visit "/stylesheets/theme.css")
          (kh/response-status-is 200)
          (kh/response-body-contains "#012345")
          (kh/response-body-contains "#abcdef")
          (kh/response-body-contains "#fedcba")
          (kh/response-body-contains "/beautiful_logo.png")))

;; 2015-08-24 DM+JC TODO: This test doesn't seem to do anything?
(fact "Correct css file is used when config includes a :theme"
      (-> (k/session (ih/build-app {:config-m {:secure "false" :theme "MY_STYLING"}}))
          (k/visit "/sign-in")
          (kh/selector-has-attribute-with-content [ks/css-link] :href "/stylesheets/application.css")))

(fact "Correct app-name is used when config includes an :app-name"
      (-> (k/session (ih/build-app {:config-m {:secure "false" :app-name "My App Name"}}))
          (k/visit "/sign-in")
          (kh/selector-includes-content [ks/sign-in-app-name] "My App Name")))

;; 06 Jul 2015
(future-fact "Replaying the same post will generate a 403 from the csrf handling"
             (-> (k/session test-app)
                 (steps/register "csrf@email.com" "valid-password")
                 (steps/sign-in "csrf@email.com" "valid-password")
                 (kh/replay-last-request)
                 (kh/response-status-is 403)))

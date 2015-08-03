(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clauth.client :as cl-client]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [stonecutter.toggles :as toggles]
            [stonecutter.config :as config]
            [stonecutter.email :as email]
            [stonecutter.integration.kerodon-helpers :as kh]
            [stonecutter.integration.kerodon-selectors :as ks]
            [stonecutter.routes :as routes]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.db.user :as user]
            [stonecutter.view.register :as register-view]))

(l/init-logger!)
(s/setup-in-memory-stores!)

(defn setup-add-client-to-user! [email client-name]
  (let [client (cl-client/register-client client-name "myclient.com")
        client-id (:client-id client)]
    (user/add-authorised-client-for-user! email client-id)))

(defn print-enlive [state]
  (prn (-> state :enlive))
  state)

(defn print-request [state]
  (prn (-> state :request))
  state)

(defn print-state [state]
  (prn state)
  state)

(defn register [state email]
  (-> state
      (k/visit "/register")
      (k/fill-in ks/registration-email-input email)
      (k/fill-in ks/registration-password-input "valid-password")
      (k/fill-in ks/registration-confirm-input "valid-password")
      (k/press ks/registration-submit)))

(defn sign-in [state email]
  (-> state
      (k/visit "/sign-in")
      (k/fill-in ks/sign-in-email-input email)
      (k/fill-in ks/sign-in-password-input "valid-password")
      (k/press ks/sign-in-submit)))

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
   :body (str email-data)})

(email/initialise! (email/bash-sender-factory "test-resources/mail_stub.sh")
                   {:confirmation test-email-renderer})

(facts "Home url redirects to sign-in page if user is not signed in"
       (-> (k/session h/app)
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/sign-in-page-body])))

(facts "User is returned to same page when email is invalid"
       (-> (k/session h/app)
           (k/visit "/register")
           (k/fill-in "Email address" "invalid-email")
           (k/press ks/registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/registration-page-body])
           (kh/selector-includes-content [ks/registration-email-validation-element] "Enter a valid email address")))

(facts "User is returned to same page when existing email is used"
       (-> (k/session h/app)
           (register "existing@user.com")
           (k/visit "/register")
           (k/fill-in ks/registration-email-input "existing@user.com")
           (k/fill-in ks/registration-password-input "password")
           (k/fill-in ks/registration-confirm-input "password")
           (k/press ks/registration-submit)
           (kh/page-uri-is "/register")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/registration-page-body])))

(facts "Registering a new user should call out to script to send a confirmation email"
       (-> (k/session h/app)
           (setup-test-directory)
           (register "new-user@email.com")
           (checks-email-is-sent "new-user@email.com")
           (teardown-test-directory)))

(facts "Register page redirects to profile-created page when registered and
       user-login is in the session so that email address is displayed on profile card"
       (-> (k/session h/app)
           (register "email@server.com")
           (k/follow-redirect)
           (kh/page-uri-is "/profile-created")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-created-page-body])

           (k/visit "/profile")
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-page-body])
           (kh/selector-includes-content [:body] "email@server.com")))

(facts "User is not confirmed when first registering for an account; Hitting the confirmation endpoint confirms the user account when the UUID in the query string matches that for the signed in user's account"
       (-> (k/session h/app)

           (setup-test-directory)

           (register "confirmation-test@email.com")

           (k/visit (routes/path :show-profile))
           (kh/selector-exists [:.clj--email-not-confirmed-message])
           (kh/selector-not-present [:.clj--email-confirmed-message])

           (k/visit (routes/path :confirm-email-with-id
                                 :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))
           (k/follow-redirect)

           (kh/page-uri-is (routes/path :show-profile))
           (kh/selector-not-present [:.clj--email-not-confirmed-message])
           (kh/selector-exists [:.clj--email-confirmed-message])

           (teardown-test-directory)))

(facts "The account confirmation flow can be followed by a user who is not signed in when first accessing the confirmation endpoint"
       (-> (k/session h/app)
           (setup-test-directory)

           (register "confirmation-test-2@email.com")
           (k/visit "/profile")
           (k/follow ks/sign-out-link)
           (k/follow-redirect)

           (k/visit (routes/path :confirm-email-with-id
                                 :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))

           (k/follow-redirect)
           (kh/page-uri-is (routes/path :confirmation-sign-in-form
                                        :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))

           (k/fill-in ks/sign-in-password-input "valid-password")
           (k/press ks/sign-in-submit)

           (k/follow-redirect)
           (kh/page-uri-is (routes/path :confirm-email-with-id
                                        :confirmation-id (get-in (parse-test-email) [:body :confirmation-id])))
           (k/follow-redirect)
           (kh/page-uri-is (routes/path :show-profile))
           (kh/selector-not-present [:.clj--email-not-confirmed-message])
           (kh/selector-exists [:.clj--email-confirmed-message])

           (teardown-test-directory)))

(facts "User is redirected to sign-in page when accessing profile page not signed in"
       (-> (k/session h/app)
           (k/visit "/profile")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/sign-in-page-body])))

(facts "User can sign in"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/response-status-is 200)
           (kh/selector-exists [ks/profile-page-body])
           (kh/selector-includes-content [:body] "email@server.com")))

(facts "User can sign out"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/visit "/profile")
           (k/follow ks/sign-out-link)
           (k/follow-redirect)
           (kh/page-uri-is "/")
           (k/follow-redirect)
           (kh/page-uri-is "/sign-in")))

(facts "Home url redirects to profile page if user is signed in"
       (-> (k/session h/app)
           (sign-in "email@server.com")
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "Home url redirects to profile page if user is registered"
       (-> (k/session h/app)
           (register "email2@server.com")
           (k/visit "/")
           (k/follow-redirect)
           (kh/page-uri-is "/profile")))

(facts "Clients appear on user profile page"
       (-> (k/session h/app)
           (register "user@withclient.com"))
       (setup-add-client-to-user! "user@withclient.com" "myapp")
       (-> (k/session h/app)
           (sign-in "user@withclient.com")
           (k/visit "/profile")
           (kh/selector-includes-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can unshare profile card"
       (-> (k/session h/app)
           (sign-in "user@withclient.com")
           (k/visit "/profile")
           (kh/selector-includes-content [ks/profile-authorised-client-list] "myapp")
           (k/follow ks/profile-authorised-client-unshare-link)
           (kh/page-uri-contains "/unshare-profile-card")
           (k/press ks/unshare-profile-card-confirm-button)
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/selector-does-not-include-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can change password"
       (-> (k/session h/app)
           (sign-in "user@withclient.com")
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
       (-> (k/session h/app)
           (register "account_to_be@deleted.com")
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
       (-> (k/session h/app)
           (k/visit "/wrong-url")
           (kh/response-status-is 404)
           (kh/selector-exists [ks/error-404-page-body])))

(fact "Error page is shown if an exception is thrown"
      (against-background
        (register-view/registration-form anything) =throws=> (Exception.))
      (-> (k/session (h/create-app {:secure "false"} :dev-mode? false))
          (k/visit "/register")
          (kh/response-status-is 500)
          (kh/selector-exists [ks/error-500-page-body]))
      (fact "if dev mode is enabled then error middleware isn't invoked (exception not caught)"
            (-> (k/session (h/create-app {:secure "false"} :dev-mode? true))
                (k/visit "/register")) => (throws Exception)))

(fact "theme.css file is generated using environment variables"
      (-> (k/session (h/create-app {:secure "false"
                                    :header-bg-color "#012345"
                                    :inactive-tab-font-color "#FEDCBA"
                                    :static-resources-dir-path "./test-resources"
                                    :logo-file-name "beautiful_logo.png"}
                                   :dev-mode? false))
          (k/visit "/stylesheets/theme.css")
          (kh/response-status-is 200)
          (kh/response-body-contains "#012345")
          (kh/response-body-contains "#fedcba")
          (kh/response-body-contains "\"/beautiful_logo.png\"")))

(fact "Correct css file is used when config includes a :theme"
      (-> (k/session (h/create-app {:secure "false" :theme "MY_STYLING"} :dev-mode? false))
          (k/visit "/sign-in")
          (kh/selector-has-attribute-with-content [ks/css-link] :href "/stylesheets/application.css")))

(fact "Correct app-name is used when config includes an :app-name"
      (-> (k/session (h/create-app {:secure "false" :app-name "My App Name"} :dev-mode? false))
          (k/visit "/sign-in")
          (kh/selector-includes-content [ks/sign-in-app-name] "My App Name")))

(future-fact "Replaying the same post will generate a 403 from the csrf handling"
             (-> (k/session h/app)
                 (register "csrf@email.com")
                 (sign-in "csrf@email.com")
                 (kh/replay-last-request)
                 (kh/response-status-is 403)))

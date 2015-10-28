(ns stonecutter.integration.kerodon.user
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clauth.client :as cl-client]
            [clojure.java.io :as io]
            [stonecutter.email :as email]
            [stonecutter.db.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.db.user :as user]
            [stonecutter.view.index :as index]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.kerodon.kerodon-checkers :as kc]
            [stonecutter.integration.kerodon.steps :as steps]
            [stonecutter.test.util.time :as test-time]))

(l/init-logger!)
(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))

(defn setup-add-client-to-user! [email client-name]
  (let [client (cl-client/register-client (s/get-client-store stores-m) client-name "myclient.com")
        client-id (:client-id client)]
    (user/add-authorised-client-for-user! (s/get-user-store stores-m) email client-id)))

(def email-sender (email/bash-sender-factory "test-resources/mail_stub.sh"))

(def test-app (ih/build-app {:stores-m stores-m :email-sender email-sender}))

(defn debug [state]
  (prn state)
  state)


(facts "User can access index page"
       (-> (k/session test-app)
           (k/visit "/")
           (kc/check-page-is :index [ks/index-page-body])))

(facts "User is returned to index page when registration is invalid"
       (-> (k/session test-app)
           (k/visit "/")
           (kc/check-and-fill-in ks/registration-email-input "invalid-email")
           (kc/check-and-press ks/registration-submit)
           (kc/check-page-is :index [ks/index-page-body])
           (kc/selector-includes-content [ks/registration-email-validation-element] "Enter a valid email address")))

(facts "User is returned to same page when existing email is used"
       (-> (k/session test-app)
           (steps/register "existing@user.com" "password")
           (steps/sign-out)
           (k/visit "/")
           (kc/check-and-fill-in ks/registration-email-input "existing@user.com")
           (kc/check-and-fill-in ks/registration-password-input "password")
           (kc/check-and-press ks/registration-submit)
           (kc/check-page-is :index [ks/index-page-body])))

(facts "Index page redirects to profile-created page and profile card is displayed"
       (-> (k/session test-app)
           (steps/register "Frank" "Lasty" "email@server.com" "valid-password")
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile-created [ks/profile-created-page-body])
           (kc/selector-includes-content [ks/profile-created-flash] "email@server.com")

           (k/visit "/profile")
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-includes-content [ks/profile-page-profile-card-email] "email@server.com")
           (kc/selector-includes-content [ks/profile-page-profile-card-name] "Frank Lasty")))

(facts "Accept invite page redirects to profile-created page"
       (let [invitation-store (:invitation-store stores-m)]
         (-> (k/session test-app)
             (steps/accept-invite "Bob" "Invitee" "valid-password" invitation-store "email-1@server.com" (test-time/new-stub-clock 0) 7)
             (kc/check-and-follow-redirect)
             (kc/check-page-is :show-profile-created [ks/profile-created-page-body])
             (kc/selector-includes-content [ks/profile-created-flash] "email-1@server.com"))))

(facts "User is redirected to index page when accessing profile page not signed in"
       (-> (k/session test-app)
           (k/visit "/profile")
           (kc/check-and-follow-redirect)
           (kc/check-page-is :index [ks/index-page-body])))

(facts "User can sign in"
       (-> (k/session test-app)
           (steps/sign-in "email@server.com" "valid-password")
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-includes-content [:body] "email@server.com")))

(facts "User can sign out"
       (-> (k/session test-app)
           (steps/sign-in "email@server.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/sign-out-link)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :index [ks/index-page-body])))

(facts "Index url redirects to profile page if user is signed in"
       (-> (k/session test-app)
           (steps/sign-in "email@server.com" "valid-password")
           (k/visit "/")
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])))

(facts "Index url redirects to profile page if user is registered"
       (-> (k/session test-app)
           (steps/register "email2@server.com" "valid-password")
           (k/visit "/")
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])))

(facts "Clients appear on user profile page"
       (-> (k/session test-app)
           (steps/register "user@withclient.com" "valid-password"))
       (setup-add-client-to-user! "user@withclient.com" "myapp")
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (kc/selector-includes-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can unshare profile card"
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (kc/selector-includes-content [ks/profile-authorised-client-list] "myapp")
           (k/follow ks/profile-authorised-client-unshare-link)
           (kc/page-uri-contains "/unshare-profile-card")
           (kc/check-and-press ks/unshare-profile-card-confirm-button)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-does-not-include-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can request a new confirmation email"
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (kc/check-and-press ks/profile-resend-confirmation-email)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-exists [ks/profile-flash-message])))

(facts "User can change password"
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/profile-change-password-link)
           (kc/check-page-is :show-change-password-form [ks/change-password-page-body])
           (kc/check-and-fill-in ks/change-password-current-password-input "valid-password")
           (kc/check-and-fill-in ks/change-password-new-password-input "new-valid-password")
           (kc/check-and-press ks/change-password-submit)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-exists [ks/profile-flash-message])))

(facts "User can delete account"
       (-> (k/session test-app)
           (steps/register "account_to_be@deleted.com" "valid-password")
           (k/visit "/profile")
           (k/follow ks/profile-delete-account-link)
           (kc/check-page-is :show-delete-account-confirmation [ks/delete-account-page-body])
           (kc/check-and-press ks/delete-account-button)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile-deleted [ks/profile-deleted-page-body])))

(facts "User can change email address"
       (-> (k/session test-app)
           (steps/sign-in "user@withclient.com" "new-valid-password")
           (k/visit "/profile")
           (k/follow ks/profile-change-email-link)
           (kc/check-page-is :show-change-email-form [ks/change-email-page-body])
           (kc/check-and-fill-in ks/change-email-input "new_email@somewhere.com")
           (k/follow ks/change-email-cancel-button)
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-includes-content [ks/profile-page-profile-card-email] "user@withclient.com")
           (kc/selector-does-not-include-content [ks/profile-page-profile-card-email] "new_email@somewhere.com")
           (k/follow ks/profile-change-email-link)
           (kc/check-page-is :show-change-email-form [ks/change-email-page-body])
           (kc/check-and-fill-in ks/change-email-input "new_email@somewhere.com")
           (kc/check-and-press ks/change-email-button)
           (kc/check-and-follow-redirect)
           (kc/selector-exists [ks/profile-page-body])
           (kc/selector-includes-content [ks/profile-unconfirmed-email-message] "new_email@somewhere.com")
           (kc/selector-includes-content [ks/profile-flash-message] "email")
           (kc/selector-includes-content [ks/profile-flash-message] "changed")
           (kc/selector-includes-content [ks/profile-page-profile-card-email] "new_email@somewhere.com")
           ))

(facts "Not found page is shown for unknown url"
       (-> (k/session test-app)
           (k/visit "/wrong-url")
           (kc/response-status-is 404)
           (kc/selector-exists [ks/error-404-page-body])))

(fact "Error page is shown if an exception is thrown"
      (against-background
        (index/index anything) =throws=> (Exception.))
      (-> (k/session (ih/build-app {:prone-stack-tracing? false}))
          (k/visit "/")
          (kc/response-status-is 500)
          (kc/selector-exists [ks/error-500-page-body]))
      (fact "if prone stack-tracing is enabled then error middleware isn't invoked (exception not caught)"
            (-> (k/session (ih/build-app {:prone-stack-tracing? true}))
                (k/visit "/")) => (throws Exception)))

(fact "theme.css file is generated using environment variables"
      (-> (k/session (ih/build-app {:config-m {:secure "false"
                                               :header-bg-color "#012345"
                                               :header-font-color "#ABCDEF"
                                               :header-font-color-hover "#FEDCBA"
                                               :static-resources-dir-path "./test-resources"
                                               :logo-file-name "beautiful_logo.png"}}))
          (k/visit "/stylesheets/theme.css")
          (kc/response-status-is 200)
          (kc/response-body-contains "#012345")
          (kc/response-body-contains "#abcdef")
          (kc/response-body-contains "#fedcba")
          (kc/response-body-contains "/beautiful_logo.png")))

(fact "Correct app-name is used when config includes an :app-name"
      (-> (k/session (ih/build-app {:config-m {:secure "false" :app-name "My App Name"}}))
          (k/visit "/")
          (kc/selector-includes-content [ks/index-app-name] "My App Name")))

;; 06 Jul 2015
(future-fact "Replaying the same post will generate a 403 from the csrf handling"
             (-> (k/session test-app)
                 (steps/register "csrf@email.com" "valid-password")
                 (steps/sign-in "csrf@email.com" "valid-password")
                 (kc/replay-last-request)
                 (kc/response-status-is 403)))

(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clauth.client :as cl-client]
            [stonecutter.integration.kerodon-helpers :as kh]
            [stonecutter.integration.kerodon-selectors :as ks]
            [stonecutter.handler :as h]
            [stonecutter.db.storage :as s]
            [stonecutter.logging :as l]
            [stonecutter.db.user :as user]
            [stonecutter.view.register :as register-view]))

(l/init-logger!)

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

(s/setup-in-memory-stores!)

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
           (k/follow [ks/profile-authorised-client-unshare-link])
           (kh/page-uri-contains "/unshare-profile-card")
           (k/press ks/unshare-profile-card-confirm-button)
           (k/follow-redirect)
           (kh/page-uri-is "/profile")
           (kh/selector-does-not-include-content [ks/profile-authorised-client-list] "myapp")))

(facts "User can delete account"
       (-> (k/session h/app)
           (register "account_to_be@deleted.com")
           (k/visit "/profile")
           (k/follow ks/delete-account-link)
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
      (-> (k/session (h/create-app :dev-mode? false))
          (k/visit "/register")
          (kh/response-status-is 500)
          (kh/selector-exists [ks/error-500-page-body]))
      (fact "if dev mode is enabled then error middleware isn't invoked (exception not caught)"
            (-> (k/session (h/create-app :dev-mode? true))
                (k/visit "/register")) => (throws Exception)))

(future-fact "Replaying the same post will generate a 403 from the csrf handling"
             (-> (k/session h/app)
                 (register "csrf@email.com")
                 (sign-in "csrf@email.com")
                 (kh/replay-last-request)
                 (kh/response-status-is 403)))

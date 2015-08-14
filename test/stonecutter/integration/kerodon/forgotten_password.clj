(ns stonecutter.integration.kerodon.forgotten-password
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.db.storage :as storage]
            [stonecutter.handler :as h]
            [stonecutter.test.email :as test-email]
            [stonecutter.routes :as r]
            [stonecutter.integration.kerodon.kerodon-checkers :as kc]
            [stonecutter.integration.kerodon.kerodon-helpers :as kh]
            [stonecutter.integration.kerodon.steps :as steps]
            [stonecutter.logging :as l]))

(l/init-logger!)
(ih/setup-db)

(def stores-m (storage/create-mongo-stores (ih/get-test-db)))
(def email-sender (test-email/create-test-email-sender))

(def base-url "https://myapp.com")
(def user-email "user@blah.com")
(def user-password "original-password")

(defn visit-email-link [state test-email-sender base-url]
  (let [pattern (re-pattern (str base-url "([^\\s]+)"))
        email-body (-> test-email-sender test-email/last-sent-email :body)]
    (->> email-body
         (re-find pattern)
         second
         (k/visit state))))

(facts "User can receive a reset password e-mail by following forgotten password link"
       (-> (k/session (h/create-app {:secure "false" :base-url base-url} stores-m email-sender))

           ; User registers and signs out
           (steps/register user-email user-password)
           (steps/sign-out)

           ; User has forgotten password so follows forgotten password link
           (k/visit (r/path :home))
           (k/follow-redirect)
           (k/follow [ks/forgotten-password-button])
           (kc/page-route-is :show-forgotten-password-form)

           ; User fills in email to receive forgotten password email
           (k/fill-in ks/forgotten-password-email user-email)
           (k/press ks/forgotten-password-submit)
           (kc/check-and-follow-redirect)
           (kc/page-route-is :show-forgotten-password-confirmation)
           (kc/response-status-is 200)
           (kc/selector-exists [ks/forgotten-password-email-sent-page-body])

           ; User receives email and follows reset link
           (visit-email-link email-sender base-url)

           ; User resets password
           (kc/page-title-is "Reset your password")
           (k/fill-in ks/reset-password-field "new-password")
           (k/fill-in ks/reset-confirm-password-field "new-password")
           (k/press ks/reset-password-submit)

           ; user is signed in to profile
           (k/follow-redirect)
           (kc/page-route-is :show-profile)

           ; User should be shown flash message informing them that there password has changed
           (kc/selector-exists [ks/profile-flash-message])

           ; When user signs out, then can sign back in with their new password
           (steps/sign-out)
           (steps/sign-in user-email "new-password")
           (k/follow-redirect)
           (kc/page-route-is :home)
           (k/follow-redirect)
           (kc/page-route-is :show-profile)

           ; If user tries to reuse reset link, they will be redirected to the forgotten password form
           (visit-email-link email-sender base-url)
           (k/follow-redirect)
           (kc/page-route-is :show-forgotten-password-form)))

(ih/teardown-db)


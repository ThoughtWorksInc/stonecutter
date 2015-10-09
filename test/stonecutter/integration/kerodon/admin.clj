(ns stonecutter.integration.kerodon.admin
  (:require [midje.sweet :refer :all]
            [stonecutter.logging :as l]
            [stonecutter.integration.integration-helpers :as ih]
            [stonecutter.db.storage :as s]
            [kerodon.core :as k]
            [stonecutter.integration.kerodon.steps :as steps]
            [stonecutter.integration.kerodon.kerodon-checkers :as kc]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]))

(l/init-logger!)
(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))

(def test-app (ih/build-app {:stores-m stores-m
                             :config-m {:secure         "false"
                                        :admin-login    "admin-user@user.com"
                                        :admin-password "password"}}))

(facts "Admin user can view the user-list page"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/users")
           (kc/response-status-is 200)))

(facts "Non-admin user cannot access the user-list page"
       (-> (k/session test-app)
           (steps/register "normal-user@user.com" "password")
           (k/visit "/admin/users")
           (kc/response-status-is 404)))

(facts "Admin can make a user trusted or untrusted"
       (-> (k/session test-app)
           (steps/register "normal-user@user.com" "password1")
           (steps/sign-out)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/users")
           (kc/check-and-press ks/user-trustworthiness-submit)
           (kc/check-and-follow-redirect)
           (kc/selector-exists ks/user-trustworthiness-flash-message)))

(facts "Admin user can view the apps page"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/response-status-is 200)))

(facts "Non-admin user cannot view the apps page"
       (-> (k/session test-app)
           (steps/sign-in "normal-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/response-status-is 404)))

(facts "Admin can add an app to the app list"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/check-and-fill-in ks/create-app-form-name "client-name")
           (kc/check-and-fill-in ks/create-app-form-url "client-url")
           (kc/check-and-press ks/create-app-form-submit)
           (kc/check-and-follow-redirect)
           (kc/selector-includes-content [ks/create-app-form-flash-message-name] "client-name")
           (kc/selector-includes-content [ks/apps-list-item-title] "client-name")
           (kc/selector-includes-content [ks/apps-list-item-url] "client-url")))

(facts "Admin cannot add apps with blank or empty fields"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/check-and-fill-in ks/create-app-form-name "   ")
           (kc/check-and-fill-in ks/create-app-form-url "   ")
           (kc/check-and-press ks/create-app-form-submit)
           (kc/check-and-follow-redirect)
           (kc/selector-does-not-include-content [ks/create-app-form-flash-message-name] "   ")
           (kc/selector-does-not-include-content [ks/apps-list-item-title] "   ")
           (kc/selector-does-not-include-content [ks/apps-list-item-url] "   ")
           (kc/check-and-press ks/create-app-form-submit)
           (kc/check-and-follow-redirect)
           (kc/selector-not-present [ks/create-app-form-flash-message-name])))

(facts "Admin can change password"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/profile")
           (kc/selector-exists [ks/apps-list-link])
           (kc/selector-exists [ks/user-list-link])
           (k/follow ks/profile-change-password-link)
           (kc/check-page-is :show-change-password-form [ks/change-password-page-body])
           (kc/selector-exists [ks/apps-list-link])
           (kc/selector-exists [ks/user-list-link])
           (kc/check-and-fill-in ks/change-password-current-password-input "password")
           (kc/check-and-fill-in ks/change-password-new-password-input "new-valid-password")
           (kc/check-and-press ks/change-password-submit)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-profile [ks/profile-page-body])
           (kc/selector-exists [ks/profile-flash-message])))
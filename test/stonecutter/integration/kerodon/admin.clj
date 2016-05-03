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

(def stores-m (s/create-mongo-stores (ih/get-test-db) (ih/get-test-db-connection) "stonecutter"))

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

(facts "Admin can add and delete an app while on the app list"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/check-and-fill-in ks/create-app-form-name "client-name")
           (kc/check-and-fill-in ks/create-app-form-url "client-url")
           (kc/check-and-press ks/create-app-form-submit)
           (kc/check-and-follow-redirect)
           (kc/selector-includes-content [ks/create-app-form-flash-message-name] "client-name")
           (kc/selector-includes-content [ks/apps-list-item-title] "client-name")
           (kc/selector-includes-content [ks/apps-list-item-url] "client-url")

           (k/follow ks/apps-list-delete-app-link)
           (kc/selector-exists [ks/delete-app-page-body])
           (k/follow ks/cancel-delete-app-link)
           (kc/check-page-is :show-apps-list [ks/apps-list-page])
           (kc/selector-not-present [ks/create-app-form-flash-message-name])
           (kc/selector-includes-content [ks/apps-list-item-title] "client-name")
           (kc/selector-includes-content [ks/apps-list-item-url] "client-url")

           (k/follow ks/apps-list-delete-app-link)
           (kc/selector-exists [ks/delete-app-page-body])
           (kc/check-and-press ks/delete-app-button)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-apps-list [ks/apps-list-page])
           (kc/selector-not-present [ks/apps-list-item-title])
           (kc/selector-not-present [ks/apps-list-item-url])
           (kc/selector-includes-content [ks/deleted-app-form-flash-message-name] "client-name")))

(facts "Admin cannot add apps with blank or empty fields"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/check-and-fill-in ks/create-app-form-name "   ")
           (kc/check-and-fill-in ks/create-app-form-url "   ")
           (kc/check-and-press ks/create-app-form-submit)
           (kc/selector-does-not-include-content [ks/create-app-form-flash-message-name] "   ")
           (kc/selector-does-not-include-content [ks/apps-list-item-title] "   ")
           (kc/selector-does-not-include-content [ks/apps-list-item-url] "   ")
           (kc/check-and-press ks/create-app-form-submit)
           (kc/selector-not-present [ks/create-app-form-flash-message-name])))

(facts "Admin can invite users"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/profile")
           (k/follow ks/invite-user-link)
           (kc/check-and-fill-in ks/invite-user-email-input "email@somewhere.com")
           (kc/check-and-press ks/invite-user-submit)
           (kc/check-and-follow-redirect)
           (kc/check-page-is :show-invite [ks/invite-user-page-body])
           (kc/selector-exists [ks/invite-user-flash-message])))

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

(ih/teardown-db)
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

(facts "Admin user can view the apps page"
       (-> (k/session test-app)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/response-status-is 200)))

(facts "Non-admin user cannot view the apps page"
       (-> (k/session test-app)
           (steps/register "normal-user@user.com" "password")
           (k/visit "/admin/apps")
           (kc/response-status-is 404)))

(facts "Non-admin user cannot access the user-list page"
       (-> (k/session test-app)
           (steps/sign-in "normal-user@user.com" "password")
           (k/visit "/admin/users")
           (kc/response-status-is 404)))

(facts "Admin can make a user trusted or untrusted"
       (-> (k/session test-app)
           (steps/register "normal-user@user.com" "password1")
           (steps/sign-out)
           (steps/sign-in "admin-user@user.com" "password")
           (k/visit "/admin/users")
           (kc/check-and-press ks/user-trustworthiness-submit)
           (kc/response-status-is 302)))



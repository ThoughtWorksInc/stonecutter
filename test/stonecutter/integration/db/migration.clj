(ns stonecutter.integration.db.migration
  (:require [midje.sweet :refer :all]
            [monger.collection :as monger-c]
            [stonecutter.integration.integration-helpers :as ith]
            [stonecutter.db.migration :as m]
            [stonecutter.db.mongo :as db]))

(defn do-with-test-db [f]
  (ith/setup-db)
  (let [db (ith/get-test-db)]
    (f db))
  (ith/teardown-db))

(defn test-migration-1 [db]
  (monger-c/insert db "fruit" {:name "orange"}))
(defn test-migration-2 [db]
  (monger-c/insert db "fruit" {:name "lemon"}))
(defn test-migration-3 [db]
  (monger-c/insert db "fruit" {:name "apple"}))

(defn count-fruit [db fruit-type]
  (monger-c/count db "fruit" {:name fruit-type}))

(def migrations [{:id "migration1"
                  :up test-migration-1}
                 {:id "migration2"
                  :up test-migration-2}])

(facts "About running migrations"
       (fact "each migration is only run once"
             (do-with-test-db
               (fn [db]
                 (do
                   (m/run-migrations db migrations)
                   (m/run-migrations db migrations))
                 (count-fruit db "orange") => 1
                 (count-fruit db "lemon") => 1
                 (count-fruit db "apple") => 0)))

       (fact "can run a new migration"
             (do-with-test-db
               (fn [db]
                 (let [new-migration {:id "migration3" :up test-migration-3}
                       updated-migrations (conj migrations new-migration)]
                   (m/run-migrations db updated-migrations)
                   (count-fruit db "orange") => 1
                   (count-fruit db "lemon") => 1
                   (count-fruit db "apple") => 1)))))

(defn valid-uuid? [s]
  (and
    (= (type s) String)
    (= (count s) 36)))

(facts "about adding uid to user collection"
       (do-with-test-db
         (fn [db]
           (monger-c/insert db "users" {:_id "email1" :login "email1" :password "p"})
           (monger-c/insert db "users" {:_id "email2" :login "email2" :password "q" :uid "a-uid"})
           (m/add-user-uids db)
           (monger-c/count db "users") => 2
           (-> (monger-c/find-map-by-id db "users" "email1") :uid) => valid-uuid?
           (-> (monger-c/find-map-by-id db "users" "email2") :uid) => "a-uid")))

(facts "about changing default role to untrusted role"
       (do-with-test-db
         (fn [db]
           (monger-c/insert db "users" {:_id "email1" :login "email1" :password "q" :uid "a-uid" :role "default"})
           (monger-c/insert db "users" {:_id "email2" :login "email2" :password "q" :uid "b-uid"})
           (monger-c/insert db "users" {:_id "email3" :login "email3" :password "q" :uid "c-uid" :role "admin"})
           (monger-c/insert db "users" {:_id "email4" :login "email4" :password "q" :uid "d-uid" :role "untrusted"})
           (m/change-default-roles-to-untrusted-roles db)
           (monger-c/count db "users") => 4
           (-> (monger-c/find-map-by-id db "users" "email1") :role) => "untrusted"
           (-> (monger-c/find-map-by-id db "users" "email2") :role) => "untrusted"
           (-> (monger-c/find-map-by-id db "users" "email3") :role) => "admin"
           (-> (monger-c/find-map-by-id db "users" "email4") :role) => "untrusted")))

(facts "about adding default user image"
       (do-with-test-db
         (fn [db]
           (monger-c/insert db "users" {:_id "email1" :login "email1" :password "q" :uid "a-uid" :role "default"})
           (m/add-default-user-profile-picture-src db)
           (-> (monger-c/find-map-by-id db "users" "email1") :profile-picture) => "/images/temp-avatar-300x300.png")))

(tabular
  (fact "about updating records to use mongo generated ids"
        (do-with-test-db
          (fn [db]
            (monger-c/insert db ?collection-name {:_id "email1" ?key-id "email1" :second-value "q"})
            (monger-c/insert db ?collection-name {:_id "12345678" ?key-id "email2" :second-value "q"})
            (m/update-records-to-use-generated-ids db)
            (monger-c/count db ?collection-name) => 2
            (let [first-record (first (monger-c/find-maps db ?collection-name {?key-id "email1"}))]
              first-record =not=> nil
              (:_id first-record) =not=> "email1"
              (:second-value first-record) => "q")
            (let [second-record (first (monger-c/find-maps db ?collection-name {?key-id "email2"}))]
              second-record =not=> nil
              (:_id second-record) => "12345678"
              (:second-value second-record) => "q"))))
  ?collection-name ?key-id
  db/user-collection db/user-primary-key
  db/token-collection db/token-primary-key
  db/confirmation-collection db/confirmation-primary-key
  db/client-collection db/client-primary-key
  db/invitation-collection db/invite-primary-key
  db/forgotten-password-collection db/forgotten-password-primary-key
  db/auth-code-collection db/auth-code-primary-key

  )


(ns stonecutter.integration.db.migration
  (:require [midje.sweet :refer :all]
            [monger.collection :as monger-c]
            [clojure.string :as s]
            [stonecutter.integration.integration-helpers :as ith]
            [stonecutter.db.migration :as m]))

(background (before :facts (ith/setup-db)
                    :after (ith/teardown-db)))

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
       (do "each migration is only run once"
             (let [db (ith/get-test-db)]
               (do
                 (m/run-migrations db migrations)
                 (m/run-migrations db migrations))
               (count-fruit db "orange") => 1
               (count-fruit db "lemon") => 1
               (count-fruit db "apple") => 0))

       (do "can run a new migration"
             (let [db (ith/get-test-db)
                   new-migration {:id "migration3" :up test-migration-3}
                   updated-migrations (conj migrations new-migration)]
               (m/run-migrations db updated-migrations)
               (count-fruit db"orange") => 1
               (count-fruit db"lemon") => 1
               (count-fruit db"apple") => 1)))

(defn valid-uuid? [s]
  (and
    (= (type s) String)
    (= (count s) 36)))

(facts "about adding uid to user collection"
       (let [db (ith/get-test-db)]
         (monger-c/insert db "users" {:_id "email1" :login "email1" :password "p"})
         (monger-c/insert db "users" {:_id "email2" :login "email2" :password "q" :uid "a-uid"})
         (m/add-user-uids db)
         (monger-c/count db "users") => 2
         (-> (monger-c/find-map-by-id db "users" "email1") :uid) => valid-uuid?
         (-> (monger-c/find-map-by-id db "users" "email2") :uid) => "a-uid"))

(facts "about changing default role to untrusted role"
       (let [db (ith/get-test-db)]
         (monger-c/insert db "users" {:_id "email1" :login "email1" :password "q" :uid "a-uid" :role "default"})
         (monger-c/insert db "users" {:_id "email2" :login "email2" :password "q" :uid "b-uid"})
         (monger-c/insert db "users" {:_id "email3" :login "email3" :password "q" :uid "c-uid" :role "admin"})
         (monger-c/insert db "users" {:_id "email4" :login "email4" :password "q" :uid "d-uid" :role "untrusted"})
         (m/change-default-roles-to-untrusted-roles db)
         (monger-c/count db "users") => 4
         (-> (monger-c/find-map-by-id db "users" "email1") :role) => "untrusted"
         (-> (monger-c/find-map-by-id db "users" "email2") :role) => "untrusted"
         (-> (monger-c/find-map-by-id db "users" "email3") :role) => "admin"
         (-> (monger-c/find-map-by-id db "users" "email4") :role) => "untrusted"))

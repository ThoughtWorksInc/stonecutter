(ns stonecutter.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [clauth.store :as cl-s]
    [monger.core :as m]
    [monger.collection :as c]
    [stonecutter.db.mongo :as mongo]))

(def test-db "stonecutter-test")
(def coll "users")

(def db (atom nil))
(def conn (atom nil))

(defn connect []
  (reset! conn (m/connect))
  (reset! db (m/get-db @conn test-db)))

(defn clear-data []
  (m/drop-db @conn test-db)
  (reset! db (m/get-db @conn test-db)))

(defn disconnect []
  (m/disconnect @conn))

(defn create-clear-user-store [db]
  (clear-data)
  (mongo/create-user-store db))

;; SETUP
(connect)


(fact "about storing"
      (let [store (create-clear-user-store @db)]
        (c/find-maps @db coll) => empty?
        (cl-s/store! store :login {:login "userA" :password "password"}) => {:login "userA" :password "password"}
        (count (c/find-maps @db coll)) => 1
        (c/find-one-as-map @db coll {:login "userA"}) => (contains {:login "userA" :password "password"})))

(fact "about fetching users"
      (let [store (create-clear-user-store @db)
            user {:login "userA" :password "passwordA"}]
        (cl-s/store! store :login user)
        (cl-s/fetch store "userA") => user))

(fact "about fetching clients"
      (let [store (mongo/create-client-store @db)
            client {:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url nil}]
        (cl-s/store! store :client-id client)
        (cl-s/fetch store "ABCDEFGHIJKLM") => client))

(fact "about viewing entries"
      (let [store (create-clear-user-store @db)]
        (c/insert @db coll {:login "userA" :password "passwordA"})
        (c/insert @db coll {:login "userB" :password "passwordB"})
        (cl-s/entries store) => (contains [{:login "userA" :password "passwordA"}
                                           {:login "userB" :password "passwordB"}]
                                          :in-any-order)))

(fact "about resetting the store"
      (let [store (create-clear-user-store @db)]
        (c/insert @db coll {:login "userA" :password "passwordA"})
        (c/insert @db coll {:login "userB" :password "passwordB"})
        (cl-s/reset-store! store)
        (c/find-maps @db coll) => empty?))

(fact "about revoking a user"
      (let [store (create-clear-user-store @db)]
        (c/insert @db coll {:_id "userA" :login "userA" :password "passwordA"})
        (c/insert @db coll {:_id "userB" :login "userB" :password "passwordB"})
        (cl-s/revoke! store "userA")
        (let [records (c/find-maps @db coll)]
          (count records) => 1
          (first records) => (contains {:login "userB" :password "passwordB"}))))

(fact "about creating mongo store from mongo uri"
      (let [store (-> "mongodb://localhost:27017/stonecutter-test" mongo/get-mongo-db create-clear-user-store)]
        (cl-s/store! store :login {:login "userA"})
        (cl-s/fetch store "userA") => {:login "userA"}))

(fact "can update a user with a supplied function"
      (let [store (create-clear-user-store @db)
            update-with-new-key (fn [user] (assoc user :new-key "new-value"))]
        (c/insert @db coll {:_id "userA" :login "userA" :password "passwordA"})
        (mongo/update! store "userA" update-with-new-key) => {:login    "userA"
                                                              :password "passwordA"
                                                              :new-key  "new-value"}
        (mongo/update! store "notUserA" update-with-new-key) => nil))

(fact "can update an email and automatically update the _id"
      (let [store (create-clear-user-store @db)
            update-with-new-login (fn [user] (assoc user :login "new-value"))]
        (c/insert @db coll {:_id "userA" :login "userA" :password "passwordA"})
        (mongo/update! store "userA" update-with-new-login) => {:login    "new-value"
                                                                :password "passwordA"}
        (cl-s/fetch store "userA") => nil
        (cl-s/fetch store "new-value") =not=> nil))

(defn run-query-tests [store type]
  (fact {:midje/name (format "can query records with a query map for %s store" type)}
        (let [geoff {:name "geoff" :fruit "apple" :colour "green"}
              barbara {:name "barbara" :fruit "apple" :colour "red"}
              derek {:name "derek" :fruit "banana" :colour "green"}]
          (cl-s/store! store :name geoff)
          (cl-s/store! store :name barbara)
          (cl-s/store! store :name derek)
          (tabular
            (fact "query returns correct result"
                  (set (mongo/query store ?query)) => (set ?result))
            ?query                            ?result
            {:fruit "apple"}                  [geoff barbara]
            {:name "derek"}                   [derek]
            {:fruit "apple" :colour "green"}  [geoff]
            {:fruit "kumquat"}                []
            ))))

(facts "run query tests for both in memory and mongo"
       (run-query-tests (mongo/create-memory-store) "Atom")
       (run-query-tests (create-clear-user-store @db) "Mongo"))

;; TEARDOWN
(disconnect)




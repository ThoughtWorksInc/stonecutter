(ns stonecutter.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [clauth.store :as s]
    [monger.core :as m]
    [monger.collection :as c]
    [stonecutter.db.mongo :refer [create-client-store create-mongo-user-store get-mongo-db]]))

(def test-db "stonecutter-test")
(def coll "users")

(def db (atom nil))
(def conn (atom nil))

(defn connect []
  (swap! conn (constantly (m/connect)))
  (swap! db (constantly (m/get-db @conn test-db))))

(defn clear-data []
  (m/drop-db @conn test-db))

(defn disconnect []
  (m/disconnect @conn))

(background
  (before :facts (do (connect) (clear-data))
   :after (do (clear-data) (disconnect))))


(facts "about storing"
       (let [store (create-mongo-user-store @db)]
         (c/find-maps @db coll) => empty?
         (s/store! store :login {:login "userA" :password "password"}) => {:login "userA" :password "password"}
         (count (c/find-maps @db coll)) => 1
         (c/find-one-as-map @db coll {:login "userA"}) => (contains {:login "userA" :password "password"})))

(facts "about fetching users"
       (let [store (create-mongo-user-store @db)
             user {:login "userA" :password "passwordA"}]
         (s/store! store :login user)
         (s/fetch store "userA") => user))

(facts "about fetching clients"
       (let [store (create-client-store @db)
             client {:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url nil}]
         (s/store! store :client-id client)
         (s/fetch store "ABCDEFGHIJKLM") => client))

(facts "about viewing entries"
       (let [store (create-mongo-user-store @db)]
         (c/insert @db coll {:login "userA" :password "passwordA"})
         (c/insert @db coll {:login "userB" :password "passwordB"})
         (s/entries store) => (contains [{:login "userA" :password "passwordA"}
                                         {:login "userB" :password "passwordB"}]
                                        :in-any-order)))

(facts "about resetting the store"
       (let [store (create-mongo-user-store @db)]
         (c/insert @db coll {:login "userA" :password "passwordA"})
         (c/insert @db coll {:login "userB" :password "passwordB"})
         (s/reset-store! store)
         (c/find-maps @db coll) => empty?))

(facts "about revoking a user"
       (let [store (create-mongo-user-store @db)]
         (c/insert @db coll {:_id "userA" :login "userA" :password "passwordA"})
         (c/insert @db coll {:_id "userB" :login "userB" :password "passwordB"})
         (s/revoke! store "userA")
         (let [records (c/find-maps @db coll)]
           (count records) => 1
           (first records) => (contains {:login "userB" :password "passwordB"}))))

(facts "about creating mongo store from mongo uri"
       (let [store (-> "mongodb://localhost:27017/stonecutter-test" get-mongo-db create-mongo-user-store)]
         (s/store! store :login {:login "userA"})
         (s/fetch store "userA") => {:login "userA"}))

(fact "check that unique index exists for 'login' field"
      (let [store (create-mongo-user-store @db)]
        (c/indexes-on @db coll) => (contains [(contains {:name "login_1"})])
        (c/insert @db coll {:login "userA"})
        (c/insert @db coll {:login "userA"}) => (throws Exception)))

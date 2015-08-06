(ns stonecutter.integration.db.mongo
  (:require
    [midje.sweet :refer :all]
    [clauth.store :as cl-s]
    [monger.core :as m]
    [monger.collection :as c]
    [stonecutter.db.mongo :refer [create-client-store create-user-store get-mongo-db update!]]))

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

(fact "about storing"
      (let [store (create-user-store @db)]
        (c/find-maps @db coll) => empty?
        (cl-s/store! store :login {:login "userA" :password "password"}) => {:login "userA" :password "password"}
        (count (c/find-maps @db coll)) => 1
        (c/find-one-as-map @db coll {:login "userA"}) => (contains {:login "userA" :password "password"})))

(fact "about fetching users"
      (let [store (create-user-store @db)
            user {:login "userA" :password "passwordA"}]
        (cl-s/store! store :login user)
        (cl-s/fetch store "userA") => user))

(fact "about fetching clients"
      (let [store (create-client-store @db)
            client {:client-id "ABCDEFGHIJKLM", :name "Green Party", :client-secret "NOPQRSTUVWXYZ", :url nil}]
        (cl-s/store! store :client-id client)
        (cl-s/fetch store "ABCDEFGHIJKLM") => client))

(fact "about viewing entries"
      (let [store (create-user-store @db)]
        (c/insert @db coll {:login "userA" :password "passwordA"})
        (c/insert @db coll {:login "userB" :password "passwordB"})
        (cl-s/entries store) => (contains [{:login "userA" :password "passwordA"}
                                           {:login "userB" :password "passwordB"}]
                                          :in-any-order)))

(fact "about resetting the store"
      (let [store (create-user-store @db)]
        (c/insert @db coll {:login "userA" :password "passwordA"})
        (c/insert @db coll {:login "userB" :password "passwordB"})
        (cl-s/reset-store! store)
        (c/find-maps @db coll) => empty?))

(fact "about revoking a user"
      (let [store (create-user-store @db)]
        (c/insert @db coll {:_id "userA" :login "userA" :password "passwordA"})
        (c/insert @db coll {:_id "userB" :login "userB" :password "passwordB"})
        (cl-s/revoke! store "userA")
        (let [records (c/find-maps @db coll)]
          (count records) => 1
          (first records) => (contains {:login "userB" :password "passwordB"}))))

(fact "about creating mongo store from mongo uri"
      (let [store (-> "mongodb://localhost:27017/stonecutter-test" get-mongo-db create-user-store)]
        (cl-s/store! store :login {:login "userA"})
        (cl-s/fetch store "userA") => {:login "userA"}))

(fact "check that unique index exists for 'login' field"
      (let [store (create-user-store @db)]
        (c/indexes-on @db coll) => (contains [(contains {:name "login_1"})])
        (c/insert @db coll {:login "userA"})
        (c/insert @db coll {:login "userA"}) => (throws Exception)))


(fact "can update a user with a supplied function"
      (let [store (create-user-store @db)
            update-with-new-key (fn [user] (assoc user :new-key "new-value"))]
        (c/insert @db coll {:_id "userA" :login "userA" :password "passwordA"})
        (update! store "userA" update-with-new-key) => {:login "userA"
                                                        :password "passwordA"
                                                        :new-key "new-value"}
        (update! store "notUserA" update-with-new-key) => nil))

(ns stonecutter.integration.mongo
  (:require
    [midje.sweet :refer :all]
    [clauth.store :as s]
    [monger.core :as m]
    [monger.collection :as c]
    [stonecutter.mongo :refer [new-mongo-store mongo-store-from-uri]]))

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
       (let [store (new-mongo-store @db)]
         (c/find-maps @db coll) => empty?
         (s/store! store "userA" {:login "userA" :password "password"}) => {:login "userA" :password "password"}
         (count (c/find-maps @db coll)) => 1
         (c/find-one-as-map @db coll {:login "userA"}) => (contains {:login "userA" :password "password"})))

(facts "about fetching"
       (let [store (new-mongo-store @db)]
         (c/insert @db coll {:login "userA" :password "passwordA"})
         (s/fetch store "userA") => {:login "userA" :password "passwordA"}))

(facts "about viewing entries"
       (let [store (new-mongo-store @db)]
         (c/insert @db coll {:login "userA" :password "passwordA"})
         (c/insert @db coll {:login "userB" :password "passwordB"})
         (s/entries store) => (contains [{:login "userA" :password "passwordA"}
                                         {:login "userB" :password "passwordB"}]
                                        :in-any-order)))

(facts "about resetting the store"
       (let [store (new-mongo-store @db)]
         (c/insert @db coll {:login "userA" :password "passwordA"})
         (c/insert @db coll {:login "userB" :password "passwordB"})
         (s/reset-store! store)
         (c/find-maps @db coll) => empty?))

(facts "about revoking a user"
       (let [store (new-mongo-store @db)]
         (c/insert @db coll {:login "userA" :password "passwordA"})
         (c/insert @db coll {:login "userB" :password "passwordB"})
         (s/revoke! store "userA")
         (let [records (c/find-maps @db coll)]
           (count records) => 1
           (first records) => (contains {:login "userB" :password "passwordB"}))))

(facts "about creating mongo store from mongo uri"
       (let [store (mongo-store-from-uri "mongodb://localhost:27017/stonecutter")]
         (s/store! store "userA" {:login "userA"})
         (s/fetch store "userA") => {:login "userA"}))
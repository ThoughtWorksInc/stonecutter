(ns stonecutter.integration.mongo
  (:require
    [midje.sweet :refer :all]
    [clauth.store :as s]
    [monger.core :as m]
    [monger.collection :as c]
    [stonecutter.mongo :refer [new-mongo-store]]))

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



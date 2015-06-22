(ns stonecutter.integration.mongo
  (:require
    [midje.sweet :refer :all]
    [clauth.store :as s]
    [monger.core :as m]
    [monger.collection :as c]
    [stonecutter.mongo :refer [new-mongo-store]]))

(def test-db "stonecutter-test")
(def conn (m/connect))
(def db (m/get-db conn test-db))



(facts "about storing"
       (let [store (new-mongo-store db)]
         (c/find-maps db "users") => empty?
         (s/store! store "userA" {:login "userA" :password "password"}) => {:login "userA" :password "password"}
         (count (c/find-maps db "users")) => 1
         (c/find-one-as-map db "users" {:login "userA"}) => (contains {:login "userA" :password "password"})))

(m/drop-db conn test-db)
(m/disconnect conn)


(ns stonecutter.test.db.expiry
  (:require [midje.sweet :refer :all]
            [stonecutter.db.mongo :as m]
            [clauth.store :as cl-store]
            [stonecutter.db.expiry :as e]
            [stonecutter.test.util.time :as test-time]
            [stonecutter.util.time :as time]
            [stonecutter.db.mongo :as mongo]))

(fact "storing record with expiry"
      (let [clock (test-time/new-stub-clock 100)
            store (m/create-memory-store)
            doc {:name "craig"}]
        (e/store-with-expiry! store clock :name doc 2000)
        (cl-store/fetch store "craig") => {:name "craig" :_expiry 2100}))

(tabular
  (fact "about fetch-with-expiry"
        (let [clock (test-time/new-stub-clock ?time)
              store (m/create-memory-store)]
          (cl-store/store! store :name {:name "craig" :_expiry 100})
          (e/fetch-with-expiry store clock "craig") => ?result
          (cl-store/entries store) => ?store-contents))
  ?time       ?result                       ?store-contents
  99          {:name "craig"}               [{:name "craig" :_expiry 100}]
  100         nil                           []
  101         nil                           []
  )

(tabular
  (fact "about query-with-expiry"
        (let [clock (test-time/new-stub-clock ?time)
              store (m/create-memory-store)]
          (cl-store/store! store :name {:name "craig" :_expiry 100})
          (e/query-with-expiry store clock :name {:name "craig"}) => ?result
          (cl-store/entries store) => ?store-contents))
  ?time       ?result                       ?store-contents
  99          [{:name "craig"}]             [{:name "craig" :_expiry 100}]
  100         []                            []
  101         []                            [])
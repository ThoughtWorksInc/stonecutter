(ns stonecutter.test.test-helpers
  (:require [ring.mock.request :as mock]))

(defn create-request
  ([method url params]
   (-> (mock/request method url)
       (assoc :params params)
       (assoc-in [:context :translator] {})))
  ([method url params session]
   (-> (create-request method url params)
       (assoc :session session))))

(defn create-request-with-query-string
  ([method url params]
   (-> (mock/request method url params)
       (assoc :params params)
       (assoc-in [:context :translator] {})))
  ([method url params session]
   (-> (create-request-with-query-string method url params)
       (assoc :session session))))

(defn create-user [login password]
  {:login    login
   :password password
   :name     nil
   :url      nil})
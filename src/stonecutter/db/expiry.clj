(ns stonecutter.db.expiry
  (:require [stonecutter.util.time :as time]
            [clauth.store :as cl-store]
            [stonecutter.db.mongo :as mongo]))

(defn expire-or-return-doc [store clock id doc]
  (if (and doc (> (:_expiry doc) (time/now-in-millis clock)))
    (dissoc doc :_expiry)
    (do (cl-store/revoke! store id) nil)))

(defn fetch-with-expiry [store clock id]
  (->> (cl-store/fetch store id)
       (expire-or-return-doc store clock id)))

(defn query-with-expiry [store clock kw query]
  (let [docs (mongo/query store query)]
    (doall (->> docs
                (map #(expire-or-return-doc store clock (kw %) %))
                (remove nil?)))))

(defn store-with-expiry! [store clock kw doc relative-expiry]
  (let [expiry (+ (time/now-in-millis clock) relative-expiry)]
    (->> (assoc doc :_expiry expiry)
         (cl-store/store! store kw))))

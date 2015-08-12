(ns stonecutter.db.expiry
  (:require [stonecutter.util.time :as time]
            [clauth.store :as cl-store]))

(defn store-with-expiry! [store clock kw doc relative-expiry]
  (let [expiry (+ (time/now-in-millis clock) relative-expiry)]
    (->> (assoc doc :_expiry expiry)
         (cl-store/store! store kw))))

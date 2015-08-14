(ns stonecutter.util.time
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]))

(def sec 1000)
(def minute (* 60 sec))
(def hour (* 60 minute))
(def day (* 24 hour))

(defn- -now-in-millis []
  (-> (t/now) c/to-long))

(defprotocol Clock
  (now-in-millis [this]))

(deftype JodaClock []
  Clock
  (now-in-millis [this]
    (-now-in-millis)))

(defn new-clock []
  (JodaClock. ))

(defn now-plus-hours-in-millis [clock plus-hours]
  (-> (now-in-millis clock)
      (c/from-long)
      (t/plus (t/hours plus-hours))
      c/to-long))

(def to-epoch c/to-epoch)

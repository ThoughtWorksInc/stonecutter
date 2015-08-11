(ns stonecutter.util.time
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn now-in-millis []
  (-> (t/now) c/to-long))

(defn now-plus-hours-in-millis [plus-hours]
  (-> (t/now) (t/plus (t/hours plus-hours))))

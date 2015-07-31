(ns stonecutter.util.uuid
  (:import (java.util UUID)))

(defn uuid []
  (str (UUID/randomUUID)))

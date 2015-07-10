(ns stonecutter.routes
  (:require [scenic.routes :as scenic]
            [bidi.bidi :as bidi]))

(def routes (scenic/load-routes-from-file "routes.txt"))

(defn path [action & params]
  (apply bidi/path-for routes action params))

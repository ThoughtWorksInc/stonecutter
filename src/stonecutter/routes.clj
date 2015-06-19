(ns stonecutter.routes
  (:require [scenic.routes :refer [load-routes-from-file]]
            [bidi.bidi :refer [path-for]]))

(def routes (load-routes-from-file "routes.txt"))

(defn path [action & params]
  (apply path-for routes action params))

(ns stonecutter.integration.kerodon
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter.handler :refer [app]]
            [stonecutter.storage :as s]
            [net.cgrand.enlive-html :as html]))

(defn p [v] (prn v) v)

(defn page-title-is [state title]
  (fact {:midje/name "Checking page title:"}
        (-> state :enlive (html/select [:title]) first html/text) => title)
  state
  )

(defn page-uri-is [state uri]
  (fact {:midje/name "Checking page uri:"}
        (-> state :request :uri) => uri))

(s/start-in-memory-datastore!)

(facts "Home url redirects to registration page"
       (-> (k/session app)
           (k/visit "/")
           (k/follow-redirect)
           (page-title-is "Register")
           (page-uri-is "/register")))

(ns stonecutter.controller.oauth
  (:require [ring.util.response :as response]
            [clauth.endpoints :as ep]))

(defn authorise [request]
  ((ep/authorization-handler {:auto-approver (constantly true)}) request))

(defn token [request]
  ((ep/token-handler) request))
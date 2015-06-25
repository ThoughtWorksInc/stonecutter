(ns stonecutter.controller.oauth
  (:require [ring.util.response :as response]
            [clauth.endpoints :as ep]))

(defn authorise [request]
  (let [response ((ep/authorization-handler {:auto-approver (constantly true)}) request)]
    (-> response
        (assoc-in [:session :client-id] (get-in request [:params :client_id])))
    ))

(defn token [request]
  ((ep/token-handler) request))
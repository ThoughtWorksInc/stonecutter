(ns stonecutter.controller.oauth
  (:require [ring.util.response :as response]
            [clauth.endpoints :as ep]
            [stonecutter.routes :refer [path]]))

(defn authorise [request]
  (prn "REQUEST" request)
  (let [response ((ep/authorization-handler {:auto-approver (constantly true)}) request)]
    (prn "RESPONSE" response)
    (-> response
        (assoc-in [:session :client-id] (get-in request [:params :client_id])))
    ))

(defn validate-token [request]
  ((ep/token-handler) request))

(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as ep]
            [stonecutter.routes :refer [path]]))

(defn authorise [request]
  (let [response ((ep/authorization-handler {:auto-approver (constantly true)}) request)]
    (-> response
        (assoc-in [:session :client-id] (get-in request [:params :client_id])))))

(defn validate-token [request]
  ((ep/token-handler) request))

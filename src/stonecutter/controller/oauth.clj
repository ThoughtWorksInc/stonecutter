(ns stonecutter.controller.oauth
  (:require [clauth.endpoints :as ep]
            [stonecutter.routes :refer [path]]))

(def auth-handler (ep/authorization-handler {:auto-approver (constantly true)
                                             :user-session-required-redirect (path :show-sign-in-form)}))

(defn authorise [request]
  (let [response (auth-handler request)]
    (-> response
        (assoc-in [:session :client-id] (get-in request [:params :client_id])))))

(defn validate-token [request]
  ((ep/token-handler) request))

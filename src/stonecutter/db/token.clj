(ns stonecutter.db.token
  (:require [clauth.token :as cl-token]))

(defn generate-login-access-token [token-store user]
  (:token (cl-token/create-token token-store nil user)))

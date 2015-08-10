(ns stonecutter.view.forgotten-password-confirmation
  (:require [stonecutter.view.view-helpers :as vh]))

(defn forgotten-password-confirmation [request]
  (vh/load-template "public/forgot-password-confirmation.html"))

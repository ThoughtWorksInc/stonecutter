(ns stonecutter.view.forgotten-password-confirmation
  (:require [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(defn forgotten-password-confirmation [request]
  (vh/load-template "public/forgot-password-confirmation.html"))

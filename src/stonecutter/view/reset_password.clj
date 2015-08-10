(ns stonecutter.view.reset-password
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.view.change-password :as cp]))

(defn set-form-action [enlive-m forgotten-password-id]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :reset-password :forgotten-password-id forgotten-password-id))))

(defn reset-password [request]
  (let [err (get-in request [:context :errors])
        forgotten-password-id (get-in request [:params :forgotten-password-id] "no-id-provided")]
    (->
     (vh/load-template "public/reset-password.html")
     (set-form-action forgotten-password-id)
     vh/add-anti-forgery
     (cp/add-errors err))))

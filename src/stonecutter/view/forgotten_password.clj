(ns stonecutter.view.forgotten-password
  (:require [stonecutter.view.view-helpers :as vh]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(defn set-form-action [enlive-m]
  (-> enlive-m
      (html/at [:.clj--forgotten-password__form] (html/set-attr :action (r/path :send-forgotten-password-email)))
      (html/at [:.clj--forgotten-password__form] (html/set-attr :method "post"))))

(defn forgotten-password-form [request]
  (-> (vh/load-template "public/forgot-password.html")
      set-form-action
      vh/add-anti-forgery))

(ns stonecutter.view.apps-list
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.config :as config]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.session :as session]))

(defn apps-list [request]
      (let [err (get-in request [:context :errors])]
           (->> (vh/load-template "public/change-password.html")
                (vh/add-script "js/main.js"))))
(ns stonecutter.controller.stylesheets
  (:require [ring.util.response :as r]
            [garden.core :as garden]
            [stonecutter.config :as config]))

(defn generate-css [config-m]
  (let [header-bg-color (config/header-bg-color config-m)
        inactive-tab-font-color (config/inactive-tab-font-color config-m)]
    (garden/css [:.header {:background-color header-bg-color}]
                [:.tabs__item {:color inactive-tab-font-color}])))

(defn theme-css [request]
  (let [config-m (:config-m request)]
    (-> (r/response (generate-css config-m))
        (r/content-type "text/css"))))

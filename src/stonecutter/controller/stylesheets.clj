(ns stonecutter.controller.stylesheets
  (:require [ring.util.response :as r]
            [garden.core :as garden]
            [stonecutter.config :as config]))

(defn generate-theme-css [config-m]
  (let [header-bg-color (config/header-bg-color config-m)
        inactive-tab-font-color (config/inactive-tab-font-color config-m)]
    (garden/css {:pretty-print? false}
      [:.header {:background-color header-bg-color}]
      [".tabs__item:not(.tabs__item--active)" {:color inactive-tab-font-color}])))

(defn theme-css [request]
  (let [config-m (get-in request [:context :config-m])]
    (-> (r/response (generate-theme-css config-m))
        (r/content-type "text/css"))))

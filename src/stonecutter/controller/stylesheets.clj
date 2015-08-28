(ns stonecutter.controller.stylesheets
  (:require [ring.util.response :as r]
            [garden.core :as garden]
            [stonecutter.config :as config]))

(defn header-bg-color-css [config-m]
  (when-let [header-bg-color (config/header-bg-color config-m)]
    (garden/css {:pretty-print? false}
                [:.header {:background-color header-bg-color}])))

(defn logo-file-name [config-m]
  (if (config/static-resources-dir-path config-m)
    (str "/" (config/logo-file-name config-m))
    "../images/logo.svg"))

(defn header-logo-css [config-m]
  (let [logo-file-name (logo-file-name config-m)]
    (garden/css {:pretty-print? false}
                [:.header__logo {:background-image      (str "url(" logo-file-name ")")}])))

(defn generate-theme-css [config-m]
  (str
    (header-bg-color-css config-m)
    (header-logo-css config-m)))

(defn theme-css [request]
  (let [config-m (get-in request [:context :config-m])]
    (-> (r/response (generate-theme-css config-m))
        (r/content-type "text/css"))))

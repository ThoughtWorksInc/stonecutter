(ns stonecutter.view.apps-list
  (:require [stonecutter.view.view-helpers :as vh]))

(defn apps-list [request]
  (->> (vh/load-template "public/admin-apps.html")
       (vh/add-script "js/main.js")
       vh/remove-work-in-progress
       vh/set-sign-out-link
       vh/set-apps-list-link
       vh/set-user-list-link))
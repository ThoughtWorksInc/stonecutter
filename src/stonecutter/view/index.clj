(ns stonecutter.view.index
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn index [request]
  (->> (vh/load-template "public/home.html")
       (vh/set-form-action [:.clj--register__form] (r/path :register-user))
       (vh/set-form-action [:.clj--sign-in__form] (r/path :sign-in))
       (vh/set-attribute [:.clj--forgot-password] :href (r/path :show-forgotten-password-form))
       vh/remove-work-in-progress))


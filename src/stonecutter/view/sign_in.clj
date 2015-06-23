(ns stonecutter.view.sign-in
  (:require [traduki.core :as t]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:form] (html/set-attr :action (r/path :sign-in))))

(defn sign-in-form [context]
  (->> (html/html-resource "public/sign-in.html")
       set-form-action
       html/emit*
       (apply str)))

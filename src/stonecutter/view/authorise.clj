(ns stonecutter.view.authorise
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--authorise__form] (html/set-attr :action (r/path :authorise-client))))

(defn set-hidden-params [params enlive-m]
  (-> enlive-m
      (html/at [:.clj--authorise-client-id__input] (html/set-attr :value (:client_id params)))
      (html/at [:.clj--authorise-response-type__input] (html/set-attr :value (:response_type params)))
      (html/at [:.clj--authorise-redirect-uri__input] (html/set-attr :value (:redirect_uri params)))))

(defn authorise-form [request]
  (let [context (:context request)
        params (:params request)
        translator (:translator context)]
    (->> (vh/load-template "public/authorise.html")
         set-form-action
         vh/add-anti-forgery
         (set-hidden-params params)
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

(ns stonecutter.view.authorise
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--authorise__form] (html/set-attr :action (r/path :authorise-client))))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m [:.clj--authorise-cancel__link] (html/set-attr :href (r/path :show-authorise-failure))))

(defn set-hidden-params [params enlive-m]
  (-> enlive-m
      (html/at [:.clj--authorise-client-id__input] (html/set-attr :value (:client_id params)))
      (html/at [:.clj--authorise-response-type__input] (html/set-attr :value (:response_type params)))
      (html/at [:.clj--authorise-redirect-uri__input] (html/set-attr :value (:redirect_uri params)))))

(defn set-hidden-clauth-csrf-token [csrf-token enlive-m]
  (-> enlive-m
      (html/at [:.clj--authorise-csrf__input] (html/set-attr :value csrf-token))))

(defn authorise-form [request]
  (let [context (:context request)
        params (:params request)
        csrf-token (or (request :csrf-token) ((request :session {}) :csrf-token))
        translator (:translator context)]
    (->> (vh/load-template "public/authorise.html")
         set-form-action
         set-cancel-link
         vh/add-anti-forgery
         (set-hidden-params params)
         (set-hidden-clauth-csrf-token csrf-token)
         vh/remove-work-in-progress
         (t/translate translator)
         html/emit*
         (apply str))))

(ns stonecutter.view.authorise
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--authorise__form] (html/set-attr :action (r/path :authorise-client))))

(defn set-cancel-link [params enlive-m]
  (let [cancel-link (str (r/path :show-authorise-failure)
                         "?client_id=" (:client_id params)
                         "&redirect_uri=" (:redirect_uri params))]
    (html/at enlive-m [:.clj--authorise-cancel__link] (html/set-attr :href cancel-link))))

(defn set-hidden-params [params enlive-m]
  (-> enlive-m
      (html/at [:.clj--authorise-client-id__input] (html/set-attr :value (:client_id params)))
      (html/at [:.clj--authorise-response-type__input] (html/set-attr :value (:response_type params)))
      (html/at [:.clj--authorise-redirect-uri__input] (html/set-attr :value (:redirect_uri params)))))

(defn set-client-name [client-name enlive-m]
  (html/at enlive-m
           [:.clj--app-name] (html/content client-name)))

(defn set-hidden-clauth-csrf-token [csrf-token enlive-m]
  (-> enlive-m
      (html/at [:.clj--authorise-csrf__input] (html/set-attr :value csrf-token))))

(defn authorise-form [request]
  (let [params (:params request)
        client-name (get-in request [:context :client :name])
        csrf-token (or (request :csrf-token) ((request :session {}) :csrf-token))]
    (->> (vh/load-template "public/authorise.html")
         set-form-action
         (set-cancel-link params)
         (set-client-name client-name)
         (set-hidden-params params)
         (set-hidden-clauth-csrf-token csrf-token)
         vh/add-anti-forgery
         vh/remove-work-in-progress)))

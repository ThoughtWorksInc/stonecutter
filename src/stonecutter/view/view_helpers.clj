(ns stonecutter.view.view-helpers
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [net.cgrand.enlive-html :as html]
            [clojure.tools.logging :as log]
            [stonecutter.translation :as t]
            [stonecutter.routes :as r]
            [stonecutter.config :as config]))

(defn anti-forgery-snippet []
  (html/html-snippet (anti-forgery-field)))

(defn add-anti-forgery [enlive-m]
  (html/at enlive-m
           [:form] (html/prepend (anti-forgery-snippet))))

(defn set-attribute [selector attr-key attr-value enlive-m]
  (html/at enlive-m selector (html/set-attr attr-key attr-value)))

(defn set-form-action
  ([path enlive-m]
   (set-form-action [:form] path enlive-m))
  ([form-selector path enlive-m]
   (set-attribute form-selector :action path enlive-m)))

(defn set-user-list-link [enlive-m]
  (html/at enlive-m
           [:.clj--user-list__link] (html/set-attr :href (r/path :show-user-list))))

(defn set-apps-list-link [enlive-m]
  (html/at enlive-m
           [:.clj--apps-list__link] (html/set-attr :href (r/path :show-apps-list))))

(defn set-sign-out-link [enlive-m]
  (html/at enlive-m
           [:.clj--sign-out__link] (html/set-attr :href (r/path :sign-out))))

(defn remove-element [enlive-m selector]
  (html/at enlive-m selector nil))

(defn set-flash-message [request message-key enlive-m]
  (if (= (:flash request) message-key)
    enlive-m
    (remove-element enlive-m [:.clj--flash-message-container])))

(defn remove-attribute [enlive-m selector attr]
  (html/at enlive-m selector (html/remove-attr attr)))

(defn remove-attribute-globally [enlive-m attr]
  (remove-attribute enlive-m [(html/attr? attr)] attr))

(defn remove-work-in-progress [enlive-m]
  (remove-element enlive-m [:.clj-wip]))

(defn add-script [script-path enlive-m]
  (let [script-tag (html/as-nodes {:tag :script :attrs {:src script-path}})]
    (html/at enlive-m [:body] (html/append script-tag))))

;;; templates

(def template-caching? (atom true))

(def template-cache (atom {}))

(defn enable-template-caching! []
  (swap! template-caching? (constantly true)))

(defn reset-template-cache! []
  (swap! template-cache (constantly {})))

(defn disable-template-caching! []
  (swap! template-caching? (constantly false))
  (reset-template-cache!))

(defn html-resource-with-log [path]
  (log/debug (format "Loading template %s from file" path))
  (html/html-resource path))

(defn load-template [path]
  (if @template-caching?
    (if (contains? @template-cache path)
      (get @template-cache path)
      (let [html (html-resource-with-log path)]
        (swap! template-cache #(assoc % path html))
        html))
    (html-resource-with-log path)))

(defn enlive-to-str [nodes]
  (->> nodes
       html/emit*
       (apply str)))

(defn set-profile-link [enlive-m]
  (html/at enlive-m
           [:.clj--profile__link] (html/set-attr :href (r/path :show-profile))))

(defn set-user-list-link [enlive-m]
  (html/at enlive-m
           [:.clj--user-list__link] (html/set-attr :href (r/path :show-user-list))))

(defn set-apps-list-link [enlive-m]
  (html/at enlive-m
           [:.clj--apps-list__link] (html/set-attr :href (r/path :show-apps-list))))

(defn set-invite-user-link [enlive-m]
  (html/at enlive-m
           [:.clj--invite__link] (html/set-attr :href (r/path :show-invite))))

(defn set-admin-links [enlive-m]
  (-> enlive-m
      set-user-list-link
      set-apps-list-link
      set-profile-link
      set-sign-out-link
      set-invite-user-link))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--invalid")))

(defn set-user-links [enlive-m]
  (-> enlive-m
      set-profile-link
      set-sign-out-link))

(defn display-admin-navigation-links [enlive-m request library-m]
  (let [role (get-in request [:session :role])
        admin-navigation-links-snippet (first (html/select library-m [:.clj--admin-navigation-links]))
        user-navigation-links-snippet (first (html/select library-m [:.clj--user-navigation-links]))]
    (html/at enlive-m [:.clj--header-nav]
             (if (= role (:admin config/roles))
               (html/content (-> admin-navigation-links-snippet
                                 set-admin-links))
               (html/content (-> user-navigation-links-snippet
                                 set-user-links))))))

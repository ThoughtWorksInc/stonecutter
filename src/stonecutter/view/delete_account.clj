(ns stonecutter.view.delete-account
  (:require [traduki.core :as t]
            [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))

(defn set-form-action [path enlive-m]
  (html/at enlive-m [:.clj--delete-account__form] (html/set-attr :action path)))

(defn set-cancel-link [path enlive-m]
  (html/at enlive-m [:.clj--delete-account-cancel__link] (html/set-attr :href path)))

(defn set-register-link [path enlive-m]
  (html/at enlive-m [:.clj--profile-deleted-next__button] (html/set-attr :href path)))

(defn delete-account-confirmation [request]
  (->> (vh/load-template "public/delete-account.html")
       (set-form-action (r/path :delete-account))
       (set-cancel-link (r/path :show-profile))
       vh/add-anti-forgery
       vh/remove-work-in-progress))

(defn profile-deleted [request]
  (->> (vh/load-template "public/profile-deleted.html")
       (set-register-link (r/path :index))
       vh/remove-work-in-progress))

(defn email-confirmation-delete-account [request]
  (let [confirmation-id (get-in request [:params :confirmation-id] "missing-confirmation-id")]
    (->> (vh/load-template "public/delete-account.html")
         (set-form-action (r/path :show-confirmation-delete
                                  :confirmation-id confirmation-id))
         (set-cancel-link (r/path :index))
         vh/add-anti-forgery
         vh/remove-work-in-progress)))

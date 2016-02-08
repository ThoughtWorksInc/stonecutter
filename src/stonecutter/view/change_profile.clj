(ns stonecutter.view.change-profile
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(def form-row-error-css-class "form-row--invalid")

(def error-translations
  {:first-name {:blank    "content:change-profile-form/change-first-name-blank-validation-message"
                :too-long "content:change-profile-form/change-first-name-too-long-validation-message"}
   :last-name  {:blank    "content:change-profile-form/change-last-name-blank-validation-message"
                :too-long "content:change-profile-form/change-last-name-too-long-validation-message"}})


(defn add-change-first-name-error [enlive-m err]
  (if-let [change-first-name-error (:first-name err)]
    (let [error-translation (get-in error-translations [:first-name change-first-name-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--first-name])
          (html/at [:.clj--change-first-name__validation] (html/set-attr :data-l8n (or error-translation "content:change-profile-form/change-name-unknown-error")))))
    enlive-m))

(defn add-change-last-name-error [enlive-m err]
  (if-let [change-last-name-error (:last-name err)]
    (let [error-translation (get-in error-translations [:last-name change-last-name-error])]
      (-> enlive-m
          (vh/add-error-class [:.clj--last-name])
          (html/at [:.clj--change-last-name__validation] (html/set-attr :data-l8n (or error-translation "content:change-profile-form/change-name-unknown-error")))))
    enlive-m))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m [:.clj--change-profile-back__link] (html/set-attr :href (r/path :show-profile))))

(defn set-translation [enlive-m text-class translation]
  (html/at enlive-m [text-class] (html/set-attr :data-l8n translation)))

(defn add-change-name-errors [enlive-m err]
  (if (empty? err)
    enlive-m
    (-> (add-change-first-name-error enlive-m err)
        (add-change-last-name-error err))))

(defn set-flash-message [enlive-m request]
  (case (:flash request)
    :name-changed (set-translation enlive-m :.clj--flash-message-text "content:flash/name-changed")
    (vh/remove-element enlive-m [:.clj--flash-message-container])))

(defn pre-fill-inputs [enlive-m request]
  (let [context (:context request)]
    (html/at enlive-m
             [:.clj--change-first-name__input] (html/set-attr :value (or (get-in request [:params :first-name]) (:user-first-name context)))
             [:.clj--change-last-name__input] (html/set-attr :value (or (get-in request [:params :last-name]) (:user-last-name context))))))

(defn change-profile-form [request]
  (let [err (get-in request [:context :errors])
        library-m (vh/load-template-with-lang "public/library.html" request)]
    (-> (vh/load-template-with-lang "public/change-profile.html" request)
        (vh/display-admin-navigation-links request library-m)
        (add-change-name-errors err)
        (vh/set-form-action [:.clj--change-name__form] (r/path :change-profile))
        (vh/set-form-action [:.clj--update-profile-picture__link] (r/path :change-profile))
        (set-flash-message request)
        (pre-fill-inputs request)
        set-cancel-link
        vh/add-anti-forgery
        (vh/add-script "js/main.js")
        vh/remove-work-in-progress)))


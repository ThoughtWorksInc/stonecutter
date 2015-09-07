(ns stonecutter.view.change-password
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(def current-password-error-translation-key
   "content:change-password-form/current-password-invalid-validation-message")

(def unknown-error-translation-key
   "content:change-password-form/unknown-error")

(def error-translations
  {:current-password {:invalid   current-password-error-translation-key
                      :blank     current-password-error-translation-key
                      :too-short current-password-error-translation-key
                      :too-long  current-password-error-translation-key}
   :new-password {:blank "content:change-password-form/new-password-blank-validation-message"
                  :too-short "content:change-password-form/new-password-too-short-validation-message"
                  :too-long "content:change-password-form/new-password-too-long-validation-message"
                  :unchanged "content:change-password-form/new-password-unchanged-validation-message"}
   :confirm-new-password {:invalid "content:change-password-form/confirm-new-password-invalid-validation-message"}})

(def error-display-order [:current-password :new-password :confirm-new-password])

(def error-highlight-selectors {:current-password [:.clj--current-password]
                                :new-password [:.clj--new-password]
                                :confirm-new-password [:.clj--confirm-new-password]})

(defn get-kv-for-key [a-map a-key]
    (when-let [v (a-key a-map)]
      [a-key v]))

(defn kv-pairs-from-map-ordered-by [the-map ordered-keys]
    (remove nil? (map #(get-kv-for-key the-map %) ordered-keys)))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class "form-row--validation-error")))

(defn highlight-errors [enlive-m err selectors]
  (let [elements-with-errors (keys err)
        selectors-to-highlight (map #(% selectors) elements-with-errors)]
    (reduce add-error-class enlive-m selectors-to-highlight)))

(defn add-errors [enlive-m err]
  (if (empty? err)
    (-> enlive-m (html/at [:.clj--validation-summary]
                          (html/remove-class "validation-summary--show")))
    (let [ordered-error-key-pairs (kv-pairs-from-map-ordered-by err error-display-order)]
      (-> enlive-m
          (html/at [:.clj--validation-summary]
                   (html/add-class "validation-summary--show"))
          (html/at [:.clj--validation-summary__item]
                   (html/clone-for [error-key-pair ordered-error-key-pairs]
                                   (html/set-attr :data-l8n (or (get-in error-translations error-key-pair)
                                                                unknown-error-translation-key))))
          (highlight-errors err error-highlight-selectors)))))

(defn add-change-password-errors [err enlive-m]
  (-> enlive-m
      (add-errors err)))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--change-password__form] (html/set-attr :action (r/path :change-password))))

(defn change-password-form [request]
  (let [err (get-in request [:context :errors])]
    (->> (vh/load-template "public/change-password.html")
         set-form-action
         set-cancel-link
         (add-change-password-errors err)
         vh/add-anti-forgery
         vh/remove-work-in-progress
         (vh/add-script "js/change_password.js")
         )))

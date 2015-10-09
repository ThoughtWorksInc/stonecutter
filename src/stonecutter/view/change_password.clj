(ns stonecutter.view.change-password
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(def current-password-error-translation-key
   "content:change-password-form/current-password-invalid-validation-message")

(def unknown-error-translation-key
   "content:change-password-form/unknown-error")

(def form-row-error-css-class "form-row--invalid")

(def error-translations
  {:current-password {:invalid   current-password-error-translation-key
                      :blank     current-password-error-translation-key
                      :too-short current-password-error-translation-key
                      :too-long  current-password-error-translation-key}
   :new-password     {:blank     "content:change-password-form/new-password-blank-validation-message"
                      :too-short "content:change-password-form/new-password-too-short-validation-message"
                      :too-long  "content:change-password-form/new-password-too-long-validation-message"
                      :unchanged "content:change-password-form/new-password-unchanged-validation-message"}})

(def error-display-order [:current-password :new-password])

(def error-highlight-selectors {:current-password [:.clj--current-password]
                                :new-password [:.clj--new-password]})

(defn get-kv-for-key [a-map a-key]
    (when-let [v (a-key a-map)]
      [a-key v]))

(defn kv-pairs-from-map-ordered-by [the-map ordered-keys]
    (remove nil? (map #(get-kv-for-key the-map %) ordered-keys)))

(defn add-error-class [enlive-m field-row-selector]
  (html/at enlive-m field-row-selector (html/add-class form-row-error-css-class)))

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

(defn add-current-password-error [enlive-m err]
  (if-let [current-password-error (:current-password err)]
    (let [error-translation (get-in error-translations [:current-password current-password-error])]
      (-> enlive-m
          (add-error-class [:.clj--current-password])
          (html/at [:.clj--current-password__validation] (html/set-attr :data-l8n (or error-translation unknown-error-translation-key)))))
    enlive-m))

(defn add-new-password-error [enlive-m err]
  (if-let [new-password-error (:new-password err)]
    (let [error-translation (get-in error-translations [:new-password new-password-error])]
      (-> enlive-m
          (add-error-class [:.clj--new-password])
          (html/at [:.clj--new-password__validation] (html/set-attr :data-l8n (or error-translation unknown-error-translation-key)))))
    enlive-m))

(defn add-validation-summary [enlive-m err]
  (if (empty? err)
    (vh/remove-element enlive-m [:.clj--validation-summary])
    (let [ordered-error-key-pairs (kv-pairs-from-map-ordered-by err error-display-order)]
      (-> enlive-m
          (html/at [:.clj--validation-summary__item]
                   (html/clone-for [error-key-pair ordered-error-key-pairs]
                                   (html/set-attr :data-l8n (or (get-in error-translations error-key-pair)
                                                                unknown-error-translation-key))))))))

(defn add-change-password-errors [err enlive-m]
  (-> enlive-m
      (add-validation-summary err)
      (add-current-password-error err)
      (add-new-password-error err)))

(defn set-cancel-link [enlive-m]
  (html/at enlive-m
           [:.clj--change-password-cancel__link] (html/set-attr :href (r/path :show-profile))))

(defn set-form-action [enlive-m]
  (html/at enlive-m [:.clj--change-password__form] (html/set-attr :action (r/path :change-password))))

(defn change-password-form [request]
  (let [err (get-in request [:context :errors])
        library-m (vh/load-template "public/library.html")]
    (->> (vh/load-template "public/change-password.html")
         set-form-action
         set-cancel-link
         (#(vh/display-admin-navigation-links % request library-m))
         (add-change-password-errors err)
         vh/add-anti-forgery
         vh/remove-work-in-progress
         (vh/add-script "js/main.js"))))


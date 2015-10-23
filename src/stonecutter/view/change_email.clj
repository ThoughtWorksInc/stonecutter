(ns stonecutter.view.change-email
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.routes :as r]
            [stonecutter.view.view-helpers :as vh]))


(def email-error-translation-key
  "content:change-password-form/current-password-invalid-validation-message")

(def unknown-error-translation-key
  "content:change-password-form/unknown-error")

(def form-row-error-css-class "form-row--invalid")

(def error-translations
  {:new-email {:blank     "content:change-password-form/new-password-blank-validation-message"
               :too-long  "content:change-password-form/new-password-too-long-validation-message"
               :unchanged "content:change-password-form/new-password-unchanged-validation-message"}})

(def error-display-order [:current-password :new-password])

(def error-highlight-selectors {:current-password [:.clj--current-password]
                                :new-password     [:.clj--new-password]})

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

(defn add-validation-summary [enlive-m err]
  (-> enlive-m
      (html/at [:.clj--validation-summary__item]
               (html/set-attr :data-l8n (or (get-in error-translations err)
                                            unknown-error-translation-key)))))

(defn set-cancel-link [enlive-m]
  enlive-m)

(defn set-form-action [enlive-m]
  enlive-m)

(defn add-change-email-errors [enlive-m err]
  (prn "Err" err)
  (if (empty? err)
    (-> enlive-m (html/at [:.clj--validation-summary]
                          (html/remove-class "validation-summary--show")))
    (add-validation-summary enlive-m err)))

(defn change-email-form [request]
  (let [err (get-in request [:context :errors])
        library-m (vh/load-template "public/library.html")]
    (-> (vh/load-template "public/change-email.html")
        set-form-action
        set-cancel-link
        (vh/display-admin-navigation-links request library-m)
        (add-change-email-errors err)
        vh/add-anti-forgery
        vh/remove-work-in-progress
        (vh/add-script "js/main.js"))))


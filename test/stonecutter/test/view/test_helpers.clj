(ns stonecutter.test.view.test-helpers
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.helper :as helper]
            [stonecutter.translation :as t]
            [stonecutter.view.forgotten-password :as fp]))

(defn create-request
  ([]
   (create-request {} nil {} {}))

  ([translator]
   (create-request translator nil {} {}))

  ([translator err]
   (create-request translator err {} {}))

  ([translator err params]
   (create-request translator err params {}))

  ([translator err params session]
   {:context {:translator translator
              :errors err}
    :params params
    :session session}))

(def no-untranslated-strings
  (let [untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+"]
    (chatty-checker [response-body] (empty? (re-seq untranslated-string-regex response-body)))))

(defn work-in-progress-removed [enlive-map]
  (empty? (html/select enlive-map [:.clj-wip])))

(defn test-translations
  ([page-name view-fn]
   (let [translator (t/translations-fn t/translation-map)]
     (test-translations page-name view-fn (create-request translator))))
  
  ([page-name view-fn request]
   (fact {:midje/name (format "there are no missing translations for page: %s" page-name)}
         (let [page (-> request
                        view-fn
                        (helper/enlive-response (:context request)) :body)]
           page => no-untranslated-strings))))

(defn enlive-m->attr [enlive-m selector attr]
  (-> enlive-m (html/select selector) first :attrs attr))

(defn has-attr? [selector attr attr-val]
  (chatty-checker [enlive-m]
                  (= attr-val (enlive-m->attr enlive-m selector attr))))

(defn element-exists? [selector]
  (chatty-checker [enlive-m]
                  (not (empty? (html/select enlive-m selector)))))

(defn element-absent? [selector]
  (chatty-checker [enlive-m]
                  (empty? (html/select enlive-m selector))))

(defn response->enlive-m [response]
  (-> response :body html/html-snippet))

(defn has-form-action?
  ([path]
   (has-form-action? [:form] path))
  
  ([form-selector path]
   (has-attr? form-selector :action path)))

(defn has-form-method?
  ([method]
   (has-form-method? [:form] method))

  ([form-selector method]
   (has-attr? form-selector :method method)))

(defn links-to? [selector path]
  (has-attr? selector :href path))

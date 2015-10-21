(ns stonecutter.translation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [traduki.core :as t]
            [taoensso.tower :as tower]
            [stonecutter.util.map :as map]))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn load-translations-from-file [file-name]
  (-> file-name
      io/resource
      slurp
      load-translations-from-string))


(defn translation-map [lang]
  (map/deep-merge
    (load-translations-from-file (str "lang/" lang ".yml"))
    (load-translations-from-file (str "lang/" lang "-client.yml"))))

(defn config-translation []
  {:dictionary                 {:en (translation-map "en")
                                ;:fi (translation-map "fi") ;; FIXME JC 21/10/2015 removed until translations are correct
                                }
   :dev-mode?                  false
   :fallback-locale            :en
   :log-missing-translation-fn (fn [{:keys [locales ks ns] :as args}]
                                 (log/warn (str "Missing translation! locales: " locales
                                                ", keys: " ks ", namespace: " ns)))})

(defmacro load-client-translations []
  (load-translations-from-file "lang/en-client.yml"))

(defn translations-fn [translation-map]
  (fn [translation-key]
    (let [key1 (keyword (namespace translation-key))
          key2 (keyword (name translation-key))
          translation (get-in translation-map [key1 key2])]
      (when-not translation (log/warn (str "No translation found for " translation-key)))
      translation)))

(defn get-locale-from-request [request]
  (if-let [session-locale (get-in request [:session :locale])]
    session-locale
    (get request :locale :en)))

(defn context-translate [enlive-m request]
  (t/translate (partial (tower/make-t (config-translation)) (get-locale-from-request request)) enlive-m))

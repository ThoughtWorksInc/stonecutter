(ns stonecutter.test.view.test-helpers
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]))

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

(ns stonecutter.test.helper
  (:require [midje.sweet :refer :all]
            [ring.util.response :as response]
            [stonecutter.helper :as h]
            [stonecutter.helper :as helper]
            [net.cgrand.enlive-html :as html]))

(fact "disabling caching should add the correct headers"
      (let [r (-> (response/response "a-response") h/disable-caching)]
        (get-in r [:headers "Pragma"]) => "no-cache"
        (get-in r [:headers "Cache-Control"]) => "no-cache, no-store, must-revalidate"
        (get-in r [:headers "Expires"]) => "0"))

(defn get-link-href [enlive-m]
  (-> enlive-m (html/select [:link]) first :attrs :href))

(defn get-response-enlive-m [response]
  (-> response :body html/html-snippet))

(fact "If context has an alternative theme, then enlive-response swaps in that one"
      (let [html "<html><head><link data-clojure-id=\"theme-link\" href=\"stylesheets/application.css\" /></head></html>"
            enlive-m (html/html-snippet html)]
        (fact "there is no theme in context"
              (-> (helper/enlive-response enlive-m {}) get-response-enlive-m get-link-href)
                  => "stylesheets/application.css")
        (fact "theme called dcent in context modifiers the header to point to dcent_theme.css"
              (-> (helper/enlive-response enlive-m {:theme {:theme "dcent"}}) get-response-enlive-m get-link-href)
                  => "stylesheets/dcent_theme.css")))

(fact "Enlive response injects the app name anywhere where class is clj--app-name"
      (let [html "<html><body><h1 class=\"clj--app-name\"></h1><span class=\"clj--app-name\"></span></body></html>"
            enlive-m (html/html-snippet html)]
        (-> (helper/enlive-response enlive-m {:theme {:app-name "My App"}}) get-response-enlive-m
            (html/select [:h1]) first html/text) => "My App"
        (-> (helper/enlive-response enlive-m {:theme {:app-name "My App"}}) get-response-enlive-m
            (html/select [:span]) first html/text) => "My App"))

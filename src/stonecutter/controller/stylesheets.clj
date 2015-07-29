(ns stonecutter.controller.stylesheets
  (:require [ring.util.response :as r]
            [garden.core :as garden]
            [com.evocomputing.colors :as colors]))

(def color-2 "#000")
(def color-1 "#fff")

(defn decrease-alpha-to-rgba [color alpha-decrement-float]
  "alpha decrement float takes floats between 0 and 1 e.g. 0.5"
  (let [adjusted-color (colors/adjust-alpha (colors/create-color color) (- alpha-decrement-float))]
    (format "rgba(%s,%s,%s,%.1f)"
            (colors/red adjusted-color) (colors/green adjusted-color)
            (colors/blue adjusted-color) (colors/rgb-int-to-unit-float (colors/alpha adjusted-color)))))

(defn darken [color darken-percent]
  (colors/rgb-hexstr (colors/darken (colors/create-color color) darken-percent)) )

(defn generate-css []
  (garden/css [:.header {:background-color color-1}]
              [:.tabs__item {:color color-2}]
              [:.tabs__item--active {:color color-1 :background-color color-2}]
              [:.tabs__link:hover {:color (darken color-1 10)
                                   :background-color (decrease-alpha-to-rgba color-2 0.5)}]))

(defn theme-css [request]
  (-> (r/response (generate-css))
      (r/content-type "text/css")))
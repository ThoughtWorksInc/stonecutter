(ns stonecutter.test.controller.stylesheets
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.controller.stylesheets :refer [generate-theme-css]]))


(fact "generate-theme-css returns the correct css string when config includes correct environment variables"
      (generate-theme-css {:header-bg-color "#ABCDEF" :inactive-tab-font-color "#FEDCBA"})
      =>".header{background-color:#abcdef}.tabs__item{color:#fedcba}")

(fact "generate-theme-css returns the correct css string when defaults are used as environment variables are missing"
      (generate-theme-css {}) => ".header{background-color:#EEE}.tabs__item{color:#404040}")

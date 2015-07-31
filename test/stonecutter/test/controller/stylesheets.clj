(ns stonecutter.test.controller.stylesheets
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.controller.stylesheets :refer [generate-theme-css header-bg-color-css
                                                        header-logo-css inactive-tab-font-color-css]]))

(fact "generate-theme-css returns the correct css string when config includes correct environment variables"
      (generate-theme-css {:header-bg-color           "#ABCDEF" :inactive-tab-font-color "#FEDCBA"
                           :static-resources-dir-path "/some/path" :logo-file-name "logo.png"})
      => (str ".header{background-color:#abcdef}"
              ".header__logo{background:url(\"logo.png\") 50% 0 no-repeat}"
              ".tabs__item:not(.tabs__item--active){color:#fedcba}"))

(fact "generate-theme-css returns the correct css string when defaults are used as environment variables are missing"
      (generate-theme-css {}) => "")

(tabular
  (fact "header-logo-css returns the correct css if a static resources directory has been set along with a logo filename"
        (header-logo-css ?config-m) => ?css)

  ?config-m                                         ?css
  {:static-resources-dir-path "/some/path"
   :logo-file-name            "some_filename.jpg"}  ".header__logo{background:url(\"some_filename.jpg\") 50% 0 no-repeat}"
  {:logo-file-name            "some_filename.jpg"}  nil
  {}                                                nil)

(tabular
  (fact "header-bg-color-css returns correct css if corresponding env variable is set or nil otherwise"
        (header-bg-color-css ?config-m) => ?css)

  ?config-m                         ?css
  {:header-bg-color "#ABCDEF"}      ".header{background-color:#abcdef}"
  {}                                nil)

(tabular
  (fact "inactive-tab-font-color-css returns correct css if corresponding env variable is set or nil otherwise"
        (inactive-tab-font-color-css ?config-m) => ?css)

  ?config-m                                 ?css
  {:inactive-tab-font-color "#FEDCBA"}      ".tabs__item:not(.tabs__item--active){color:#fedcba}"
  {}                                        nil)

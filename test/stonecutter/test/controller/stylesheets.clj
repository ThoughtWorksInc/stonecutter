(ns stonecutter.test.controller.stylesheets
  (:require [midje.sweet :refer :all]
            [stonecutter.controller.stylesheets :refer [generate-theme-css header-bg-color-css
                                                        header-logo-css inactive-tab-font-color-css]]))

(defn gen-header-logo [path]
  (format ".header__logo{background-image:url(%s)}" path))

(def default-header-logo (gen-header-logo "../images/logo.svg"))

(fact "generate-theme-css returns the correct css string when config includes correct environment variables"
      (generate-theme-css {:header-bg-color           "#ABCDEF" :inactive-tab-font-color "#FEDCBA"
                           :static-resources-dir-path "/some/path" :logo-file-name "logo.png"}) ; TODO fix me
      => (every-checker (contains ".header{background-color:#abcdef}")
                        (contains (gen-header-logo "/logo.png"))
                        (contains ".tabs__item:not(.tabs__item--active){color:#fedcba}")))

(fact "generate-theme-css returns the correct css string when defaults are used as environment variables are missing"
      (generate-theme-css {}) =>  ".header__logo{background-image:url(../images/logo.svg)}")

(tabular
  (fact "header-logo-css returns the correct css if a static resources directory has been set along with a logo filename"
        (header-logo-css ?config-m) => ?css)

  ?config-m                                         ?css
  {:static-resources-dir-path "/some/path"
   :logo-file-name            "some_filename.jpg"}  (gen-header-logo "/some_filename.jpg")
  {:logo-file-name            "some_filename.jpg"}  default-header-logo
  {}                                                default-header-logo)

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

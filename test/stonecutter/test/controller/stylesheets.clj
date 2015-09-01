(ns stonecutter.test.controller.stylesheets
  (:require [midje.sweet :refer :all]
            [stonecutter.controller.stylesheets :refer [generate-theme-css header-bg-color-css
                                                        header-logo-css header-font-color-css]]))

(defn gen-header-logo [path]
  (format ".header__logo{background-image:url(%s)}" path))

(def default-header-logo (gen-header-logo "../images/logo.svg"))

(fact "generate-theme-css returns the correct css string when config includes correct environment variables"
      (generate-theme-css {:header-bg-color "#ABCDEF"
                           :header-font-color "#123456"
                           :header-font-color-hover "#654321"
                           :static-resources-dir-path "/some/path"
                           :logo-file-name "logo.png"}) ; TODO fix me
      => (every-checker (contains ".header{background-color:#abcdef}")
                        (contains (gen-header-logo "/logo.png"))
                        (contains ".header-nav__link{color:#123456")
                        (contains ".header-nav__link:hover{color:#654321")))

(fact "generate-theme-css returns the correct css string when defaults are used as environment variables are missing"
      (generate-theme-css {}) =>  (str ".header{background-color:#eee}"
                                       ".header__logo{background-image:url(../images/logo.svg)}"
                                       ".header-nav__link{color:#222}"
                                       ".header-nav__link:hover{color:#00d3ca}"))

(tabular
  (fact "header-logo-css returns the correct css if a static resources directory has been set along with a logo filename"
        (header-logo-css ?config-m) => ?css)
  ?config-m                                         ?css
  {:static-resources-dir-path "/some/path"
   :logo-file-name            "some_filename.jpg"}  (gen-header-logo "/some_filename.jpg")
  {:static-resources-dir-path "/some/path"}         default-header-logo
  {:logo-file-name            "some_filename.jpg"}  default-header-logo
  {}                                                default-header-logo)

(tabular
  (fact "header-bg-color-css returns correct css if corresponding env variable is set or default otherwise"
        (header-bg-color-css ?config-m) => ?css)
  ?config-m                         ?css
  {:header-bg-color "#ABCDEF"}      ".header{background-color:#abcdef}"
  {}                                ".header{background-color:#eee}")

(tabular
  (fact "header-font-color-css returns correct css if corresponding env variables are set or defaults otherwise"
        (header-font-color-css ?config-m) => ?css)
  ?config-m                                  ?css
  {:header-font-color       "#ABCDEF"
   :header-font-color-hover "#FEDCBA"}      ".header-nav__link{color:#abcdef}.header-nav__link:hover{color:#fedcba}"
  {:header-font-color       "#ABCDEF"}      ".header-nav__link{color:#abcdef}.header-nav__link:hover{color:#00d3ca}"
  {:header-font-color-hover "#FEDCBA"}      ".header-nav__link{color:#222}.header-nav__link:hover{color:#fedcba}"
  {}                                        ".header-nav__link{color:#222}.header-nav__link:hover{color:#00d3ca}")


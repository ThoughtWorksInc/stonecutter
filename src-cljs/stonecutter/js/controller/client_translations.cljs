(ns stonecutter.js.controller.client_translations
  (:require [taoensso.tower :as tower])
  (:require-macros [taoensso.tower :as tower-macros]))

(def ^:private tconfig
  {:fallback-locale :en
   :compiled-dictionary (tower-macros/dict-compile "lang/client_translations.clj")})

(def t (tower/make-t tconfig))

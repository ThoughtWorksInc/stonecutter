(ns stonecutter.js.dom.common
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.translation :as t]))

(def translations (t/load-client-translations))

(defn get-lang []
  (keyword (.getAttribute (dm/sel1 :html) "lang")))

(defn add-class! [selector css-class]
  (d/add-class! (dm/sel1 selector) css-class))

(defn remove-class! [selector css-class]
  (d/remove-class! (dm/sel1 selector) css-class))

(defn set-text! [selector message]
  (d/set-text! (dm/sel1 selector) message))

(defn focus-on-element! [sel]
  (when-let [e (dm/sel1 sel)]
    (.focus e)))

(defn prevent-default-submit! [submitEvent]
  (.preventDefault submitEvent))

(defn get-value [selector]
  (d/value (dm/sel1 selector)))

(defn get-file [selector]
  (.item (.-files (dm/sel1 selector)) 0))
(ns stonecutter.js.dom.upload-photo
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(def update-image-input-selector :#profile-photo)

(defn upload-image [e]
  (.submit (dm/sel1 :.card__photo-upload)))
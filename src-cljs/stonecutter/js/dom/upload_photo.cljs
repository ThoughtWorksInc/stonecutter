(ns stonecutter.js.dom.upload-photo
  (:require [dommy.core :as d])
  (:require-macros [dommy.core :as dm]))

(def update-image-input-selector :#profile-photo)
(def profile-card-image-selector :.card__photo)

(defn upload-image [e]
  (.submit (dm/sel1 :.card__photo-upload)))

(defn show-button [e]
  (d/remove-attr! (dm/sel1 :.card__photo-upload) :hidden))

(defn hide-button [e]
  (d/set-attr! (dm/sel1 :.card__photo-upload) :hidden "hidden"))
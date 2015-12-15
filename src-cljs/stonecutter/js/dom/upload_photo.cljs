(ns stonecutter.js.dom.upload-photo
  (:require [dommy.core :as d]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.controller.client_translations :as ct])
  (:require-macros [dommy.core :as dm]
                   [stonecutter.config :as config]))

(def update-image-input-selector :#profile-photo)
(def profile-card-image-selector :.card__photo)

(defn selected-file-size []
  (.-size (.item (.-files (dm/sel1 :#profile-photo)) 0)))

(defn upload-image [e]
  (if (< (selected-file-size) (config/image-upload-size-limit))
    (.submit (dm/sel1 :.card__photo-upload))
    (do (d/remove-attr! (dm/sel1 :.clj--profile-image-error-container) :hidden)
        (d/set-text! (dm/sel1 :.clj--profile-image-error-text) (ct/t (dom/get-lang) :image-error/too-large)))))

(defn show-button [e]
  (d/remove-attr! (dm/sel1 :.card__photo-upload) :hidden))

(defn hide-button [e]
  (d/set-attr! (dm/sel1 :.card__photo-upload) :hidden "hidden"))
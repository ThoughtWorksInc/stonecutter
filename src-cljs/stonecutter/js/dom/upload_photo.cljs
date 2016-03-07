(ns stonecutter.js.dom.upload-photo
  (:require [dommy.core :as d]
            [stonecutter.validation :as v]
            [stonecutter.js.dom.common :as dom]
            [stonecutter.js.controller.change-profile-form :as cpfc])
  (:require-macros [dommy.core :as dm]))

(def profile-card-photo__input :.clj--card-photo-input)
(def profile-card-photo__selector :.clj--card-photo)
(def profile-card-photo__form :.clj--card-photo-upload)
(def profile-card-photo__error-container :.clj--profile-image-error-container)
(def profile-card-photo__error-text :.clj--profile-image-error-text)

(defn submit-form! [selector]
  (.submit (dm/sel1 selector)))

(defn upload-image [e]
  (let [image (dom/get-file profile-card-photo__input)
        error (v/validate-profile-picture image)]
    (if (and image (not error))
      (submit-form! profile-card-photo__form)
      (do (d/remove-attr! (dm/sel1 profile-card-photo__error-container) :hidden)
          (d/set-text! (dm/sel1 profile-card-photo__error-text)
                       (get-in cpfc/error-to-message [:change-profile-picture error]))))))

(defn show-button [e]
  (d/remove-attr! (dm/sel1 profile-card-photo__form) :hidden))

(defn hide-button [e]
  (d/set-attr! (dm/sel1 profile-card-photo__form) :hidden "hidden"))
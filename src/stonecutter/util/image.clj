(ns stonecutter.util.image
  (:require [image-resizer.util :as resizer-u]
            [image-resizer.core :as resizer]
            [pantomime.media :as mt]
            [pantomime.mime :as mime]
            [stonecutter.config :as config]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (javax.imageio ImageIO)
           (org.apache.commons.io IOUtils)
           (org.apache.commons.codec.binary Base64)
           (java.awt.image BufferedImage)))

(def image-height 150)
(def image-width 150)

(defn picture-data [picture]
  (last (str/split picture #",")))

(defn picture-type [picture]
  (->> picture
       (re-find #"/(\w{3,});")
       last
       str/upper-case))

(defn check-file-type [request]
  (when (not (mt/image? (get-in request [:params :profile-photo :content-type])))
    :not-image))

(defn check-file-extension [request]
  (when (empty? (mime/extension-for-name (get-in request [:params :profile-photo :content-type])))
    :unsupported-extension))

(defn check-file-size [request]
  (let [file (get-in request [:params :profile-photo :tempfile])
        file-size (.length (io/file file))]
    (when (> file-size (config/image-upload-size-limit))
      :too-large)))

(defn remove-transparency [transparent-image]
  (let [opaque-image (BufferedImage. image-width image-height BufferedImage/TYPE_3BYTE_BGR)
        input-rgb (.getRGB transparent-image 0 0 image-width image-height nil 0 image-width)]
    (.setRGB opaque-image 0 0 image-width image-height input-rgb 0 image-width)
    opaque-image))

(defn buffered-image->input-stream [buffered-image content-type]
  (let [os (ByteArrayOutputStream.)
        file-extension (mime/extension-for-name content-type)
        opaque-image (remove-transparency buffered-image)]
    (ImageIO/write opaque-image (.substring file-extension 1) os)
    (ByteArrayInputStream. (.toByteArray os))))

(defn resize-and-crop-image [file]
  (let [image (resizer-u/buffered-image file)
        dimensions (resizer/dimensions image)
        width (first dimensions)
        height (last dimensions)]
    (if (< width height)
      (-> image
          (resizer/resize-to-width image-width)
          (resizer/crop-to-height image-height))
      (-> image
          (resizer/resize-to-height image-height)
          (resizer/crop-to-width image-width)))))

(defn encode-base64 [file]
  (->> file
       .getInputStream
       IOUtils/toByteArray
       Base64/encodeBase64
       (map char)
       (apply str "data:" (.getContentType file) ";base64,")))
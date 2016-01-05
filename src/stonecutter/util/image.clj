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
           (org.apache.commons.codec.binary Base64)))

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

(defn buffered-image->input-stream [buffered-image content-type]
  (let [os (ByteArrayOutputStream.)
        file-extension (mime/extension-for-name content-type)]
    (ImageIO/write buffered-image (.substring file-extension 1) os)
    (ByteArrayInputStream. (.toByteArray os))))

(defn resize-and-crop-image [file]
  (let [image (resizer-u/buffered-image file)
        dimensions (resizer/dimensions image)
        width (first dimensions)
        height (last dimensions)]
    (if (< width height)
      (-> image
          (resizer/resize-to-width 150)
          (resizer/crop-to-height 150))
      (-> image
          (resizer/resize-to-height 150)
          (resizer/crop-to-width 150)))))

(defn encode-base64 [file]
  (->> file
       .getInputStream
       IOUtils/toByteArray
       Base64/encodeBase64
       (map char)
       (apply str "data:" (.getContentType file) ";base64,")))

(defn picture-data [picture]
  (last (str/split picture #",")))

(defn picture-type [picture]
  (str/upper-case (last (re-find #"/(\w{3,});" picture))))
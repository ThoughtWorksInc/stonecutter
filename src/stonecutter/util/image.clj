(ns stonecutter.util.image
  (:require [clojure.string :as string]
            [image-resizer.util :as resizer-u]
            [image-resizer.core :as resizer])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (javax.imageio ImageIO)
           (org.apache.commons.io IOUtils)
           (org.apache.commons.codec.binary Base64)))

(defn content-type->file-extension [content-type]
  (let [image-type (keyword (last (string/split content-type #"/")))]
    (image-type {:jpeg "jpg"
                 :gif  "gif"
                 :png  "png"})))

(defn buffered-image->input-stream [buffered-image content-type]
  (let [os (ByteArrayOutputStream.)
        file-extension (content-type->file-extension content-type)]
    (ImageIO/write buffered-image file-extension os)
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
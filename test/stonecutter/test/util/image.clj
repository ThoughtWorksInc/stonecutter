(ns stonecutter.test.util.image
  (:require [midje.sweet :refer :all]
            [stonecutter.util.image :as i]))

(tabular
  (fact "the correct file type is extracted from the image encoding"
        (i/picture-type ?image) => ?filetype)
  ?image                                  ?filetype
  "data:image/jpeg;base64,ABCDEFGHIJKL"   "JPEG"
  "data:image/png;base64,MNOPQRSTUVWXYZ"  "PNG")
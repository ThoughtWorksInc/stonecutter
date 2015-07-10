(ns stonecutter.test.helper
  (:require [midje.sweet :refer :all]
            [ring.util.response :as response]
            [stonecutter.helper :refer [disable-caching]]))

(fact "disabling caching should add the correct headers"
      (let [r (-> (response/response "a-response") disable-caching)]
        (get-in r [:headers "Pragma"]) => "no-cache"
        (get-in r [:headers "Cache-Control"]) => "no-cache, no-store, must-revalidate"
        (get-in r [:headers "Expires"]) => "0"))

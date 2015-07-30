(ns stonecutter.test.toggles
  (:require [midje.sweet :refer :all]
            [stonecutter.toggles :as toggles]))

(fact "story-25 should be deactivated"
      toggles/story-25 => :deactivated)

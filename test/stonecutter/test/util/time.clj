(ns stonecutter.test.util.time
  (:require [stonecutter.util.time :as time]))

(defprotocol SettableClock
  (update-time [this f]))

(deftype StubClock [time-atom]
  time/Clock
  (now-in-millis [this]
    @time-atom)
  SettableClock
  (update-time [this f]
    (swap! time-atom f)))

(defn new-stub-clock [start-time-millis]
  (StubClock. (atom start-time-millis)))
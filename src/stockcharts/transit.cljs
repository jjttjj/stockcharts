(ns stockcharts.transit
  (:require [alandipert.storage-atom :as storage]
            [cognitect.transit :as transit]
            [cljs-time.core :as time])
  (:import
   [goog.date Date DateTime UtcDateTime Interval]))

;;;;;transit cljs-time
(def transit-readers
  {"m" (transit/read-handler (fn [s] (UtcDateTime.fromTimestamp s)))})


(def transit-writers
  {UtcDateTime (transit/write-handler
                (constantly "m")
                (fn [v] (.getTime v))
                (fn [v] (str (.getTime v))))
   DateTime    (transit/write-handler
                (constantly "m")
                (fn [v] (.getTime (time/to-utc-time-zone v)))
                (fn [v] (str (.getTime (time/to-utc-time-zone v)))))})

(swap! storage/transit-read-handlers merge transit-readers)
(swap! storage/transit-write-handlers merge transit-writers)

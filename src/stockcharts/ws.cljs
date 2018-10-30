(ns stockcharts.ws
  (:require [clojure.string :as str]
            ["socket.io-client" :as io]
            [stockcharts.util :as u]))

;;Incoming messages not properly oganized or attributable to their caller
;;works for the use case of only using tickers for a single chart, but needs
;;a more elaborate scheme otherwise to tag outgoing requests and route incoming messages

(def url-base "https://ws-api.iextrading.com/1.0")

(def sock-last (io (str url-base "/last") #js{:autoConnect false}))
(def sock-deep (io (str url-base "/deep")))

(def last-cbs (atom {}))

;;todo: account for other types of messages, or no function found in last-cbs
(defn route-msg [msg]
  (let [msg (u/json-str->edn msg)]
    ((get @last-cbs (:symbol msg)) msg)))

(defn init-last! []
  (.on sock-last "connect"
       #(.on sock-last "message" route-msg))
  (.connect sock-last))

(init-last!)

(defn subscribe-last [sym-str cb]
  (println "subscribing: " sym-str)
  ;;(.off sock-last "message" cb)
  (swap! last-cbs assoc sym-str cb)
  (.emit sock-last "subscribe" sym-str))

(defn unsubscribe-last [sym-str]
  (.emit sock-last "unsubscribe" sym-str))

(ns stockcharts.state
  (:require [rum.core :as rum]
            [alandipert.storage-atom :as storage]
            [clj-iex.query :as query :refer [q]]
            [stockcharts.util :as u :refer [spy spyc]]
            [stockcharts.ws :as ws]
            [taoensso.truss :as truss :refer-macros (have have! have?)]
            [goog.array :as garr]
            [clojure.set :as set]
            [clojure.walk :as walk]
            ["lunr" :as lunr]
            [cognitect.transit :as transit]
            [cljs-time.core :as time]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            [clojure.string :as str]
            [javelin.core :refer [defc defc= cell cell=]]
            [alandipert.storage-atom :as storage-atom :refer [local-storage]]
            [stockcharts.transit :as rum-transit]
            [stockcharts.data :as data])
  (:import
   [goog.date Date DateTime UtcDateTime Interval]))

(def ALL-SYMBOLS (storage/local-storage (cell {}) ::ALL-SYMBOLS))
(def SYM-STREAM  (storage/local-storage (cell ()) ::sym-stream))

(defn valid-symbol? [sym]
  (contains? (set (keys @ALL-SYMBOLS))
             (str/upper-case sym)))

(defc= SYM-HISTORY (distinct (map :symbol SYM-STREAM)))

(defc LUNR-INDEX nil)

;;data stores
(def QUOTES (storage/local-storage (cell {}) ::quotes))
(def INTRADAY (storage/local-storage (cell {}) ::INTRADAY))
(def TICKS (cell {}))

;;user data
(def PORTFOLIOS (storage/local-storage (cell {}) ::portfolios))


(defn str->portfolio [s]
  (into {}
        (for [sym (->> (str/split s #"\W+|,")
                       (map str/upper-case)
                       (filter valid-symbol?))]
          [sym {:qty 0}])))


;;upserts a portfolio
(defn new-portfolio! [id display-name ticker-str]
  (swap! PORTFOLIOS
         assoc id
         (str->portfolio ticker-str)))



;;current page state
(defc SYM nil)

(defc CURRENT {})

(defc OHLC nil)
(defc SETTINGS {:timeframe "1y"})


;;queries/subscriptions
(defc= LIVE-QUOTES
  (into {}
        (for [[sym {:keys [previousClose] :as quote}] QUOTES
              :let [TICKS (map :price (get TICKS sym))
                    close (or (last TICKS) (:latestPrice quote))
                    change (- close previousClose)]]
          [sym
           (when (and quote TICKS)
             {:open           (:open quote)
              :high           (apply max (conj TICKS (:high quote) close))
              :low            (apply min (conj TICKS (:low quote) close))
              :close          close
              ;;What should date be here? 
              :date           (tc/from-long (:latestUpdate quote))
              :change         change
              :change-percent (/ change previousClose)})])))

(defc= OHLC+LIVE
  (let [today-bar (get LIVE-QUOTES SYM)
        ohlc (get CURRENT :ohlc)]
    (when (and ohlc today-bar)
      (if (time/= (tc/to-local-date (:date today-bar))
                  (tc/to-local-date (:date (last ohlc))))
        ohlc
        (conj ohlc today-bar)))))


;;INIT
(defn init-all-symbols []
  (q :ref-data/symbols {} #(reset! ALL-SYMBOLS (u/index-by :symbol (u/json-str->edn %)))))

(defn make-lunr-index [symbol-db]
  (lunr (fn []
          (this-as this
            (.ref this "symbol")
            (.field this "name")
            (.field this "symbol")
            (doseq [doc (vals symbol-db)]
              (.add this (clj->js doc)))))))

(defn init-lunr-index [] (reset! LUNR-INDEX (make-lunr-index @ALL-SYMBOLS)))

(u/add-initfn! (fn []
                 (when (empty? @ALL-SYMBOLS)
                   (println "initalizing ALL-SYMBOLS")
                   (init-all-symbols))))

(u/do-watch ALL-SYMBOLS
            (fn [o n] (when n (init-lunr-index))))

(defn search-symbols [searchq]
  (let [;;searchq (spy (-> searchq (str/replace #"\W|$" "+ ")))
        exactsym (.search @LUNR-INDEX (str "symbol:" searchq))
        wildsym  (.search @LUNR-INDEX (str "symbol:" searchq "*"))
        company  (.search @LUNR-INDEX (str "name:" searchq "*"))
        combined (js->clj (garr/concat exactsym wildsym company) :keywordize-keys true)]
    (map @ALL-SYMBOLS (distinct (map :ref combined)))))

(def memo-search (memoize search-symbols))

(add-watch SYM ::sym
           (fn [k r old-sym new-sym]

             ;;(remove-ticker old-sym TICKS)
             ;;don't remove tickers for now.
             (data/add-ticker new-sym TICKS)
             
             (data/fetch-current-symbol new-sym @SETTINGS CURRENT) 

             (swap! SYM-STREAM conj (spy {:type   :set-symbol
                                          :symbol new-sym
                                          :time   (time/now)}))))


(defn populate-data [syms]
  (when-let [needs-quotes (not-empty (remove (set (keys @QUOTES)) syms))]
    (data/fetch-quotes needs-quotes QUOTES))
  (when-let [needs-intraday (not-empty (remove (set (keys @INTRADAY)) syms))]
    (data/fetch-intraday-closes needs-intraday INTRADAY)))

(u/do-watch SYM-HISTORY
            (fn [o new-hist]
              (populate-data new-hist)))


(defc= WATCHED (mapcat keys (vals PORTFOLIOS)))

(u/do-watch WATCHED
            (fn [o new-symbols]
              (populate-data new-symbols)
              (data/add-tickers new-symbols TICKS)))

(def quote-refresher
  (js/setInterval
   (fn []
     (println "refreshing quotes")
     (data/fetch-quotes (keys @QUOTES) QUOTES))
   (* 1000 60 5)))

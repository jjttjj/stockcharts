(ns stockcharts.data
  (:require [clj-iex.query :as query]
            [stockcharts.util :as u :refer [spy spyc]]
            [stockcharts.ws :as ws]
            [taoensso.truss :as truss :refer-macros (have have! have?)]
            [goog.array :as garr]
            [clojure.set :as set]
            [clojure.walk :as walk]
            ["lunr" :as lunr]
            [cljs-time.core :as time]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            [clojure.string :as str]
            [javelin.core :as j :refer [defc defc= cell cell=]]
            [alandipert.storage-atom :as storage-atom :refer [local-storage]]
            [stockcharts.transit :as rum-transit]))

(defn parse-date
  ([date-str]
   (parse-date date-str nil))
  ([date-str time-str]
   (let [[_ year month day] (->> (str/split date-str #"(\d{4})-?(\d{2})-?(\d{2})")
                                 (map js/parseInt))
         [hour minute]      (when time-str
                              (->> (str/split time-str #":")
                                   (map js/parseInt)))]
     (time/local-date-time year month day hour minute)
     ;;(time/date-time year month day hour minute)

     ;;this was done to allow tranist to write dates in local-storage atom
     ;;not sure if it breaks something
     ;;(js/Date. year month day hour minute)
     )))

(defn fetch-quotes [syms result-cell]
  ;;(have #(< (count %) 100) syms) ;;TODO: batch requests for > 100
  ;;(println "Fetching quotes for" syms)
  (run!
   (fn [syms]
     (query/q :stock/batch
       {:symbol  "market"
        :symbols (str/join "," syms)
        :types   "quote"}
       #(swap! result-cell
               merge
               ;;pretty hacky way to have only top level
               ;;symbol keys not be keywordized
               (u/index-by :symbol (map :quote (vals (u/json-str->edn %)))))))
   (partition-all 100 syms)))

(defn compact-pts [pts]
  (if (< (count pts) 50)
    pts ;;TODO: note, this branch never executes, see below wrt compaction before removal
    (take-nth 15 pts)))

(defn format-intraday-results [r]
  (into {}
        (for [[sym v]      r
              [_chart pts] v]
          [sym
           (->> pts

                compact-pts
                ;;TODO: points are compacted BEFORE bad points are removed in next step
                ;;sometimes results in very few points. Switching order is less efffienct
                ;;and doesn't seem to change results for low data point stocks
                (mapv (comp #(-> %
                                 ;;(remove #(zero? ()))
                                 ;;TODO: REFACTOR!!!
                                 (assoc :marketClose (if (zero? (:marketClose %))
                                                       (:close %)
                                                       (:marketClose %)))
                                 (set/rename-keys {:marketClose :close})
                                 
                                 (assoc :date (parse-date (:date %)
                                                          (:minute %)))
                                 (dissoc :minute))
                            walk/keywordize-keys))
                

                
                
                (filter :close)
                (filter :date))])))

(defn fetch-intraday-closes [syms result-cell]
  ;;(println "Fetching intraday closes for" syms)
  ;;(have #(< (count %) 100) syms) ;;TODO: batch requests for > 100)
  (run!
   (fn [syms]
     (query/q :stock/batch
       {:symbol  "market"
        :symbols (str/join "," syms)
        :types   "chart"
        :filter  "close,marketClose,date,minute"
        :range   "1d"
        ;;:date "20180813"
        }
       
       #(swap! result-cell
               merge
               ;;pretty hacky way to have only top level
               ;;symbol keys not be keywordized
               (format-intraday-results (js->clj (.parse js/JSON %))))))
   (partition-all 100 syms)))


(defn process-pt [pt]
  (-> pt
      (update :date
              #(if-let [time-str (:minute pt)]
                 (parse-date % time-str)
                 (parse-date %)))
      (set/rename-keys {:marketOpen  :open
                        :marketHigh  :high
                        :marketLow   :low
                        :marketClose :close
                        })))

(defn process-data [data]
  (->> data
       (map process-pt)
       (remove #(or (neg? (:high %)) (neg? (:low %))))
       vec))

(defn fetch-current-symbol [sym opt result-cell]
  (query/q :stock/batch {:types  "chart,company,news,stats,financials"
                         :symbol sym
                         :range  (:timeframe opt)}
    (fn [result]
      (let [{:keys [chart company news stats financials] :as data} (u/json-str->edn result)]
        (swap! result-cell merge (-> data
                                     (set/rename-keys {:chart :ohlc})
                                     (update :ohlc process-data)))))))

(defn add-ticker [sym result-cell]
  (println "adding ticker for" sym)
  (when-not (get @result-cell sym)
    (swap! result-cell assoc sym [])
    (ws/subscribe-last sym (fn [tick]
                             (swap! result-cell update (:symbol tick) conj tick)))))

;;todo: this can be done in one call i believe
(defn add-tickers [syms result-cell]
  (doseq [sym syms]
    (add-ticker sym result-cell)))

(defn remove-ticker [sym result-cell]
  (when (get @result-cell sym)
    (ws/unsubscribe-last sym)
    (swap! result-cell dissoc sym)))



;;(list->portfolio 
(defn get-list [list-key result-cell]
  (query/q :stock.market/list {:listname (name list-key)}
    (fn [result]
      (->> result
           u/json-str->edn
           (map :symbol)
           (reset! result-cell)))))

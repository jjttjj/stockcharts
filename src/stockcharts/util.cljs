(ns stockcharts.util
  (:require [cljs.pprint :as pp])
  (:require-macros [stockcharts.util])
  (:import (goog.i18n NumberFormat)
           (goog.i18n.NumberFormat Format)))

(defn spy [x] (println x) x )
(defn spyc [x] (js/console.log x) x)

(defn json-str->edn [json] (js->clj (.parse js/JSON json) :keywordize-keys true))

(defn index-by
  "Returns a map from the result of calling f on each item in coll to that item."
  [f coll]
  (into {}
        (for [item coll]
          [(f item) item])))

(def start-time (atom nil))
(def last-measure (atom nil))

(defn start-measure [name]
  (reset! start-time (js/performance.now))
  (reset! last-measure name))

(defn stop-measure []
  (let [last @last-measure]
    (when @last-measure
      (js/window.setTimeout (fn []
                              (reset! last-measure nil)
                              (let [stop (js/performance.now)]
                                (js/console.log last "took" (- stop @start-time))))
                            0))))

(defn add-initfn!
  "Executes a function once the window load event is fired."
  [f] (.addEventListener js/window "load" #(stockcharts.util/with-timeout 0 (f))))


;;from Hoplon
(defn do-watch
  "Adds f as a watcher to ref and evaluates (f init @ref) once. The watcher
  f is a function of two arguments: the previous and next values. If init is
  not provided the default (nil) will be used."
  ([ref f]
   (do-watch ref nil f))
  ([ref init f]
   (let [k (gensym)]
     (f init @ref)
     (add-watch ref k (fn [_ _ old new] (f old new)))
     k)))

;;;Ui stuff
(defn rgb
  ([r g b]
   (rgb r g b 1))
  ([r g b a]
   (str "rgb(" r "," g "," b "," a ")")))

(defn change->opacity [pct]
  (min (-> pct js/Math.abs (* 10) (+ 0.2) ) 0.7))

(defn change->color [pct]
  (rgb (if (neg? pct) 255 0)
       0
       (if (pos? pct) 255 0)
       (change->opacity pct)))

(let [fmt-short (NumberFormat. Format/COMPACT_SHORT)]
  (defn compact-num [n] (.format fmt-short n)))

(defn format-percent [n]
  (pp/cl-format nil "~,2F%" (* n 100)))

(defn format-decimal [n]
  (pp/cl-format nil "~,2F" n))

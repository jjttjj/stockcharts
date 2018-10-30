(ns stockcharts.chart
  (:require [stockcharts.util :as u :refer [spy spyc]]
            [rum.core :as rum]
            [goog.object :as gobj]
            ["react" :as react]
            ["react-dom" :as react-dom]

            ["react-stockcharts" :as rsc]
            ["react-stockcharts/lib/series" :as series]
            ["react-stockcharts/lib/axes" :as axes]
            ["react-stockcharts/lib/helper" :as helper]
            ["react-stockcharts/lib/utils" :as rsc-utils]
            ["react-stockcharts/lib/scale" :as rsc-scale]
            ["react-stockcharts/lib/coordinates" :as coord]
            ["react-stockcharts/lib/tooltip" :as tool]
            
            ["d3-shape" :as d3shape]
            ["d3-time" :as d3t]
            ["d3-time-format" :as d3tf]
            ["d3-scale" :as d3s]
            ["d3-format" :as d3f]
            
            [cljs-time.core :as time]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]))

(def ce react/createElement)

(defn get-dimensions [el]
  (let [w (.-parentNode.clientWidth el)
        h (.-parentNode.clientHeight el)]))

(rum/defcs rum-candles <
  rum/reactive
  (rum/local 400 ::width)
  (rum/local 400 ::height)
  {:did-mount (fn [state]
                (let [cmp (:rum/react-component state)
                      width (::width state)
                      height (::height state)
                      f (fn []
                          (when-let [el (rum/dom-node state)]
                            (reset! width (.-parentNode.clientWidth el))
                            (reset! height (.-parentNode.clientHeight el))))]
                  (js/window.addEventListener "resize" f)
                  ;;can't get it working without a timeout here.
                  #_(u/with-timeout 100
                    ;;(f)
                    (js/window.dispatchEvent (js/Event. "resize"))
                    )
                  (assoc state ::resizer f)))
   #_#_:after-render (fn [state]
                   ;;(u/with-timeout 100 ((::resizer state)))
                   (js/window.dispatchEvent (js/Event. "resize"))
                   state)
   :will-unmount (fn [state]
                   (js/window.removeEventListener "resize" (::resizer state))
                   (dissoc state ::resizer))}


  [state data]
  (when-let [initial-data  (clj->js (rum/react data))]
    (let [x-scale-provider
          (rsc-scale/discontinuousTimeScaleProvider.inputDateAccessor
           #(.-date %))
          this        state
          scale-stuff (when initial-data
                        (x-scale-provider initial-data))
          
          data       (when initial-data (.-data scale-stuff))
          x-scale    (.-xScale scale-stuff)
          x-accessor (.-xAccessor scale-stuff)

          x-extents          #js[(x-accessor (last data))
                                 (x-accessor (aget data
                                                   (max 0 (- (count data) 50))))]
          display-x-accessor (.-displayXAccessor scale-stuff)
          ;; data initial-data
          ;; x-scale (d3s/scaleTime)
          ;; x-accessor  #(.-date %)
          
          
          cc (ce rsc/ChartCanvas
                 #js{:width            (rum/react (::width state)) 
                     :height           400 ;;(rum/react (::height state))
                     :ratio            1
                     :seriesName       "STOCKOHLC"
                     :data             data
                     :type             "svg"
                     :margin           #js{:left 0 :right 50 :top 10 :bottom 30}
                     :xScale           x-scale
                     :xAccessor        x-accessor
                     ;;xExtents cause chart to reset view on data update
                     ;;can get around this by storing view info in react state
                     :xExtents         x-extents
                     :displayXAccessor display-x-accessor
                     :padding          5   ;;#js{:right 10}
                     }
                 (ce rsc/Chart #js{ ;;:id       0
                                   :key      (str (gensym))
                                   :yExtents #(array (.-high %) (.-low %) )
                                   :padding  #js{:top 40 :bottom 20}}
                     #js[(ce axes/XAxis #js{:key    (str (gensym))
                                            :axisAt "bottom" :orient "bottom"
                                            :ticks  8})
                         (ce axes/YAxis #js{:key    (str (gensym))
                                            :axisAt "right" :orient "right"
                                            ;;:ticks  5
                                            })
                         (ce coord/MouseCoordinateY
                             #js{:key           (str (gensym))
                                 :at            "right"
                                 :orient        "right"
                                 :displayFormat (d3f/format ".4s")
                                 :arrowWidth    0})
                         (ce coord/MouseCoordinateY
                             #js{:key           (str (gensym))
                                 :at            "left"
                                 :orient        "right"
                                 :displayFormat (d3f/format ".4s")
                                 :arrowWidth    0})
                         (ce coord/MouseCoordinateX
                             #js{:key    (str (gensym))
                                 :at     "bottom"
                                 :orient "bottom"
                                 :displayFormat
                                 (d3tf/timeFormat "%Y-%m-%d")})

                         (ce coord/EdgeIndicator
                             #js{:key        (str (gensym))
                                 :itemType   "last"
                                 :edgeAt     "right"
                                 :orient     "right"
                                 :arrowWidth 0
                                 :yAccessor  #(.-close %)})

                         (ce series/CandlestickSeries
                             #js{:key               (str (gensym))
                                 :clip              false
                                 :fill              #(if (> (.-close %)
                                                            (.-open %))
                                                       "white"
                                                       "black")
                                 :candleStrokeWidth 1

                                 ;;use this to have sensible width for intraday
                                 ;;charts: 
                                 ;;:width 10
                                 ;;:widthRatio 1

                                 ;;:opacity 1
                                 :stroke "black"})
                         (ce coord/CrossHairCursor #js{:key "crosshaircursor"})
                         (ce tool/OHLCTooltip #js{:key "tooltip1"
                                                  :forChart    1
                                                  :origin #js[0 0]
                                                  ;;:accessor #(spy %)

                                                  ;;TODO:reset ohlctooltip
                                                  ;;when data/symbol changes
                                                  :displayValuesFor
                                                  #(or
                                                    (.-currentItem %2)
                                                    (last data))})]))]
      cc)))







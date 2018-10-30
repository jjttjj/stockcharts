(ns stockcharts.core
  (:require [rum.core :as rum]
            [stockcharts.chart :as chart]
            [garden.core :refer [css]]
            [stockcharts.state :as state]
            [stockcharts.util :as u :refer [spy spyc change->color compact-num
                                            format-decimal format-percent]]
            [clojure.string :as str]
            [javelin.core :as j]))


(declare attach-body)

;;shadow
(defn ^:dev/after-load start [] (attach-body))
(defn ^:export init [] (start))
(defn ^:dev/before-load stop [])


(j/defc tab :portfolios)
(j/defc current-portfolio (some-> @state/PORTFOLIOS first key))

(defn set-current-sym! [sym]
  (reset! state/SYM sym)
  (reset! tab :analysis))


(rum/defc sym-buttons < rum/reactive [{:keys [title src on-select]}]
  [:div 
   [:div {:style {:font-size "10px"}} title]
   (for [sym (rum/react src)]
     [:button.pure-button
      {:style {:border    "1px solid black"
               ;;:cursor       "pointer"
               ;;:margin-right "2px"
               ;;:height       "20px"
               :margin    "1px"
               :padding   "2px"
               :max-width "55px"
               
               ;;:box-sizing "border-box"
               :background-color
               (change->color (get-in (rum/react state/QUOTES)
                                      [sym :changePercent]))}
       :on-mouse-enter #(set! (.-style.borderWidth (.-currentTarget %))
                              "2px")
       :on-mouse-leave #(set! (.-style.borderWidth (.-currentTarget %))
                              "1px")
       :on-click       #(on-select sym)
       ;;#(reset! state/SYM sym)
       }
      sym])])

;;subscribes to: searchq (local). memosearch? 
;;changes: searchq local
(rum/defcs search-elem <
  ;;rum/reactive ;;only for disabled check
  (rum/local "" ::searchq)
  [state opts]
  (let [{:keys [result-width input-width
                commit-fn size placeholder src] :as opts}
        (merge {:placeholder "search for a stock"
                :result-width 400
                :input-width 30}
               opts)
        searchq (::searchq state)
        results (rum/derived-atom
                 [searchq] ::search-results
                 (fn [q] (when (not-empty q)
                           (take 10 (state/memo-search q)))))]
    ;;todo: wrap absolute/relative
    [:div {:style {:position "relative" :display "inline-block"}}
     [:input.form-control
      {:placeholder placeholder
       :type        "text"
       ;;:disabled    (not (rum/react state/search-ready?))
       :on-change   #(reset! searchq (.-target.value %))
       :on-key-up   #(when (= (.-which %) 13)
                       (when-let [sym (first @results)]
                         (commit-fn (:symbol sym))
                         (reset! searchq "")))
       :value       @searchq
       :size        size
       :style {:width input-width}
       }]
     (when-let [results @results]
       [:div {:style {:position "absolute"
                      :background-color "white"
                      :z-index 999999
                      :top "100%"
                      :left 0
                      :right 0}}
        (for [{sym :symbol company-name :name :as result} results]
                  [:div {:style {:width         result-width
                                 :text-overflow "ellipsis"
                                 :overflow      "hidden"
                                 :white-space   "nowrap"
                                 :border        "1px solid black"
                                 :background-color "white"}
                 
                         :key            (str "result-" sym)
                         :on-mouse-enter #(set! (.-style.backgroundColor (.-currentTarget %))
                                                "lightblue")
                         :on-mouse-leave #(set! (.-style.backgroundColor (.-currentTarget %))
                                                "white")
                         :on-click       #(do (commit-fn sym)
                                              (reset! searchq ""))}
                   [:div {:style {:width         "60px"         
                                  :display       "inline-block"
                                  :padding-right "5px"}}
                    sym]
                   [:div {:style {:width        "350px"
                                  :display      "inline-block"
                                  :textOverflow "ellipsis"
                                  :color        "lightgrey"}}
                    company-name]])
        [:div {:style {:border "1px solid black" :width result-width
                       :background-color "white"}}
         (sym-buttons {:title     "search history:"
                       :src       (j/cell= (take 10 state/SYM-HISTORY))
                       :on-select (fn [sym]
                                    (spy sym)
                                    (commit-fn sym)
                                    (reset! searchq ""))})]])
     ]))


(rum/defc dump-table < rum/reactive [{:keys [title src]}]
  (let [src (if (satisfies? IDeref src)
              (rum/react src)
              src)]
    [(when title [:h2 title])
     [:table.table.table-bordered.table-sm
      [:tbody
       (for [[k v] (sort-by (comp str/lower-case name key) src)]
         [:tr
          [:td (name k)]
          [:td (if (and (number? v) (> (count (str v)) 4))
                 (compact-num v)
                 (str v))]])]]]))

(defn points->svg [pts fill & [opts]]
  (let [{:keys [height width]} (merge {:height 20 :width 30} opts )
        max-price              (apply max (map :close pts))
        min-price              (apply min (map :close pts))
        view-height            (- max-price min-price)
        view-points            (map #(- max-price (:close %)) pts)        
        ]
    [:svg {:viewBox             (str "0 0 " (dec (count pts)) " " view-height)
           :height height
           :width width
           :preserveAspectRatio "none"}
     [:polygon
      {:fill            fill :stroke "none" :stroke-width "1"
       :shape-rendering "crispEdges"
       :points          (str (->> view-points
                                  (map-indexed (fn [i x]
                                                 (str i "," x)))
                                  (str/join " "))
                             " " (dec (count pts)) "," view-height
                             " " 0 "," view-height)}]]))

;;todo: add a few points from prior day?
(rum/defc svg-charts < rum/reactive [title data]
  [:div {:style {:font-size "10px"}}
   title
   [:div {:style {:line-height 0 :font-size 0}}
    (for [sym (rum/react data)]
      [:div {:style {:display     "inline-block"
                     :width       "30px"
                     :height      "20px"
                     :line-height "normal"
                     :font-size   "1em"
                     ;;:margin-bottom "0px"
                     ;;:vertical-align "top"
                     :box-sizing  "content-box"
                     ;;:border      "1px solid gray"
                     }}
       [:div {:style {:width 30
                      :height 20
                      :text-align "center"
                      :font-size   "10px"
                      :font-family "monospace"
                      :position    "absolute"
                      :vertical-align "middle"
                      :top "30%"
                      :opacity     0.4
                      ;;:display "flex"
                      ;;:align-items "center"
                      ;;:justify-content "center"
                      }}
        ;;sym
        ]
       [:div {:style {:font-size "1em" :line-height "normal"}}
        (points->svg (get (rum/react state/INTRADAY) sym)
                     (str (change->color
                           (get-in (rum/react state/QUOTES)
                                   [sym :changePercent]))))]])]])


(def cols [:symbol :qty :price :c])

;;TODO: local for Add field too, commit when enter pressed on qty field
(rum/defcs portfolio-table < rum/reactive
  (rum/local "" ::searchq)
  (rum/local nil ::add-qty)
  (rum/local [] ::columns)
  [state portfolio-key]
  ;;can/should this be a local-cell?
  (let [portfolio (j/cell= (get state/PORTFOLIOS portfolio-key)
                           #(swap! state/PORTFOLIOS assoc portfolio-key %))
        quotes (j/cell= (select-keys state/LIVE-QUOTES
                                     (keys portfolio)))
        track-qty? true]
       [:div ;;{:style {:overflow-x "auto"}}
        [:h2 portfolio-key]
        [:div
         #_[:label {:for (str portfolio-key "-portfolio")}
            [:input {:id   (str portfolio-key "-portfolio")
                     :type "checkbox"}]
            "portfolio? "]] 
        [#_#_:div.table-responsive {:style {;;:overflow-y "visible !important"
                                        ;;:overflow "visible"
                                        ;;:position "absolute"
                                        }}
         ;;the responsive table allows side scrolling, but
         ;;breaks the search-elem from overlaying everything.
         [:table.table-bordered.table-hover.table-sm
          ;;{:style {:table-layout "fixed"}}
          [:thead.thead-light
           [:tr
            [:td {:scope "col" :style {:position "sticky" :left 0 :background-color "white"}}
             "sym"]
            [:td {:scope "col"} "price"]
            [:td {:scope "col"} "change%"]
            [:td {:scope "col"} "change"]
            [:td {:scope "col"} "today"]
            (when track-qty?
              [[:td {:scope "col"} "qty"]
               [:td {:scope "col"} "PnL"]
               [:td {:scope "col"} "value"]])
            [:td {:scope "col"}]]]
          [:tbody
           (for [[sym {:keys [qty]}] (rum/react portfolio)
                 :let                [{:keys [close change-percent change] :as quote}
                                      (get (rum/react quotes) sym)]]
             [:tr
              [:td {:style          {:cursor "pointer"
                                     :position "sticky" :left 0
                                     :background-color "#DCDCDC"}
                    :on-click       #(set-current-sym! sym)
                    :on-mouse-enter #(set! (.-style.backgroundColor (.-currentTarget %))
                                           "lightgrey")
                    :on-mouse-leave #(set! (.-style.backgroundColor (.-currentTarget %))
                                           "#DCDCDC")}
               sym]
              ;;border-left is what's effecting space on right side for some reason
              [:td {:align "right"} close]
              [:td {:align "right" :style {:background-color (change->color change-percent)
                                           :font-weight      "bold"
                                           ;;:text-shadow "1px 1px white"
                                           }}
               (format-percent change-percent)]
              [:td {:align "right"} (format-decimal change)]
              [:td {:style {:padding 0 :vertical-align "bottom"}}
               (points->svg (get (rum/react state/INTRADAY) sym)
                            (str (change->color change-percent))
                            {:width  50
                             :height 20})]
              (when track-qty?
                
                ;;Qty
                [[:td {:style {:padding 0}}
                  [:input {:type        "text"
                           :placeholder 0
                           :value       qty
                           :size        2
                           :style       {:margin  0
                                         :padding 0}
                           :on-change
                           #(some->> (.-target.value %)
                                     not-empty
                                     js/parseInt
                                    
                                     (swap! portfolio assoc-in [sym :qty]))}]]

                 ;;PnL
                 [:td {:align "right"} (format-decimal (* change qty))]
                 ;;value
                 [:td {:align "right"} (format-decimal (* close qty))]])
              [:td {:align          "right"
                    :on-click       #(swap! portfolio dissoc sym)
                    :on-mouse-enter  #(set! (.-style.backgroundColor (.-currentTarget %))
                                           "lightgrey")
                    :on-mouse-leave #(set! (.-style.backgroundColor (.-currentTarget %))
                                           "white")
                    :style          {:cursor     "pointer"
                                     :width      15
                                     :text-align "center"} } "X"]])
           (let [add-qty (::add-qty state)]
             [:tr
              [:td
               {:col-span 2
                :style {
                        :position         "sticky" :left 0
                        :background-color "#DCDCDC"}}
               [:div.form-inline
                (search-elem {:commit-fn   (fn [sym]
                                             (swap! portfolio update-in [sym :qty] + @add-qty)
                                             (reset! add-qty nil))
                              :placeholder "Add"
                              :size        2
                              :input-width 60
                              })
                [:input.form-control
                 {:type        "text"
                  :placeholder "Qty"
                  
                  :value     @add-qty
                  :size      2
                  :style     {:margin  0
                              :padding 0
                              :width   40
                              ;;:border "none"
                              }
                  :on-change #(some->> (.-target.value %)
                                       not-empty
                                       js/parseInt
                                       (reset! add-qty))}]]]
              [:td]

              [:td "(total%)"]
              [:td (format-decimal (reduce + (map :change (vals (rum/react quotes)))))]
              (when track-qty?
                [[:td]
                 [:td]
                 [:td]])
              [:td]])]]]]))

(rum/defcs portfolio-form <
  (rum/local "" ::portfolio-name)
  (rum/local "" ::portfolio-str)
  [state {:keys                                                        [on-save]
          :as opts}]
  (let [portfolio-name (::portfolio-name state)
        portfolio-str  (::portfolio-str state)]
    
    [:div.pure-form
     [:h2 "new portfolio"]
     [:div.form-group
      [:input.form-control {:type      "text" :placeholder "portfolio name"
                            :on-change #(reset! portfolio-name (.-target.value %))}]
      [:textarea.form-control {:placeholder "new portfolio:
ex: AMZN, GOOG, AAPL"
                               :on-change   #(reset! portfolio-str (.-target.value %))
                               :value       @portfolio-str}]
      [:button.btn.btn-primary {:style    {:display "block"
                                       :border  "1px solid black"}
                            :on-click #(do
                                         (state/new-portfolio! @portfolio-name
                                                                @portfolio-name
                                                                @portfolio-str)
                                         (reset! current-portfolio @portfolio-name)
                                         (reset! portfolio-str "")
                                         (reset! portfolio-name ""))}
       "save"]]]))

(rum/defc quick-stats-table < rum/reactive
  [{:keys [title stats quote]}]
  (let [stats-keys [:dividendRate]
        quote-keys [:peRatio :marketCap :week52Low :week52High]
        quick-stats (merge
                     (select-keys stats stats-keys)
                     (select-keys quote quote-keys))]
    (dump-table {:title title :src quick-stats})))

(rum/defcs dump-current-stats < (rum/local false ::open?) rum/reactive
  [state {:keys [title src]}]
  (let [open? (::open? state)]
    [:div {:style {:cursor "pointer"}
           :on-click #(swap! open? not)}
     [:strong title]
     [:button {:style {:border "1px solid black"
                       :font-weight "bold"
                       :width 30}
               :on-mouse-enter #(set! (.-style.backgroundColor (.-currentTarget %))
                                      "lightgrey")
               :on-mouse-leave #(set! (.-style.backgroundColor (.-currentTarget %))
                                      "white")}
      (if @open? "-" "+")]
     [:div {:style {:display (if @open? "block" "none")}}
      
      [:div {:style {:display "inline-block" :width "50%"}}
       (dump-table {:title "stats"
                    :src   (rum/cursor-in src [:stats])})]
      [:div {:style {:display "inline-block" :width "50%"}}
       (dump-table {:title "financials"
                    :src   (rum/cursor-in src [:financials
                                               :financials
                                               0])})]]]))

(rum/defc ohlc-table < rum/reactive [{:keys [title src]}]
  (let [{:keys [changePercent open high low close]} (rum/react src)]
    [:table.table.table-bordered.table-sm
     [:tr [:td "change%"] [:td "open"] [:td "high"] [:td "low"] [:td "close"]]
     [:tbody
      [:tr
       [:td {:style {:background-color (change->color changePercent)}}
        (format-percent changePercent)]
       [:td open]
       [:td high]
       [:td low]
       [:td close]]
      ]]))

(rum/defc analysis-elem < rum/reactive
  {:after-render (fn [state]
                   (js/window.dispatchEvent (js/Event. "resize"))
                   state)}
  []
  (when-let [sym (rum/react state/SYM)]
    (let [current (rum/react state/CURRENT)]
      [
       [:h2.card-title
        {:style {:font-size 20 :text-align "center" :font-weight "bold"}}
        (rum/react state/SYM)]
       ;;i think this needs a box wrapper if non 100%
       [:div.container-fluid
        (chart/rum-candles state/OHLC+LIVE)]
       (ohlc-table {:title "" :src (rum/cursor-in state/QUOTES [sym])})
       
       [:p.card-text (-> current :company :description)]
       [:hr]
       "News:"
       [:ul
        (for [{:keys [datetime headline source url summary related image]}
              (-> current :news)]
          [:li
           [:a {:href url :target "_blank"}
            headline]
           "(" source ")"])]
       [:hr]
       [:div {:style {:display "inline-block" :width "20%"}}
        (quick-stats-table {:title "quick stats"
                            :stats (:stats current)
                            :quote (get (rum/react state/QUOTES) sym)})]
       [:div {:style {:display "inline-block" :width "70%"}}
        (dump-current-stats {:title "All Stats..." :src state/CURRENT})]])))

(rum/defc portfolios-tab < rum/reactive []
  (let [portfolios (rum/react state/PORTFOLIOS)
        current (rum/react current-portfolio)]
    [:div
     [:ul.nav.nav-tabs
       (for [[name _] portfolios]
         [:li.nav-item
          [:a.nav-link
           {:class (if (= (spy current) (spy name)) "active" nil)
            :on-click #(reset! current-portfolio name)}
           name]])
      [:li.nav-item [:a.nav-link
                     {:class    (if (nil? current) "active" nil)
                      :on-click #(reset! current-portfolio nil)}
                     "+"]]]
     (if current
       (portfolio-table current)
       (portfolio-form))
     (when current
       [:button.btn.btn-danger {:type     "button"
                                :on-click #(do (swap! state/PORTFOLIOS dissoc current)
                                               (reset! current-portfolio
                                                       (some-> portfolios first key)))}
        "delete"])]))

(rum/defcs page < rum/reactive
  [state]

  (let [current-tab (rum/react tab)
        current-sym (rum/react state/SYM)]
    [:div.container-fluid
     [:div ;;.box {:style {:width "100%" :height "30px" :border-bottom "1px solid black"}}
      [:div {:style {:padding 4} :align "center"}
       (search-elem {:input-width 400 :commit-fn #(set-current-sym! %)})]]


     [:ul.nav.nav-tabs
      [:li.nav-item
       [:a.nav-link#portfolios-tab
        {:class    (if (= current-tab :home) "active" nil)
         :on-click #(reset! tab :home)}
        "home"]]
      [:li.nav-item
       [:a.nav-link#portfolios-tab
        {:class (if (= current-tab :portfolios) "active" nil)
         :on-click #(reset! tab :portfolios)}
        "portfolios"]]
      (when current-sym
        [:li.nav-item#analysis-tab
         [:a.nav-link
          
          {:style {#_#_:background-color
                   (change->color (get-in (rum/react state/QUOTES)
                                          [current-sym :changePercent]))
                   :font-weight "bold"}
           :class    (if (= current-tab :analysis) "active" nil)
           :on-click #(reset! tab :analysis)}
          current-sym]])]

     (case current-tab
       :analysis
       [:div.card {:style {:border-top "none"}}
        ;;{:hidden (if (not= tab :analysis) "hidden")}
        [:div.card-body
         (analysis-elem)]]
       :portfolios
       [:div.card {:style {:border-top "none"}}
        [:div.card-body
         (portfolios-tab)]
        ]
       :home
       [:div.card {:style {:border-top "none"}}
        [:div.card-body
         "home"]])     
     [:div "Data provided for free by "
      [:a {:href   "https://iextrading.com/developer"
           :target "_blank"}
       "IEX"]
      ". View IEX's "
      [:a {:href   "https://iextrading.com/api-exhibit-a/"
           :target "_blank"}
       "Terms of Use"]
      "."]]))

(defn add-css [& css-strs]
  (.appendChild js/document.head
                (doto (js/document.createElement "style")
                  (.appendChild (js/document.createTextNode
                                 (str/join "\n" css-strs))))))

(defn attach-body []
  (add-css (css [:.card-body {:padding ".25rem"}]
                [:.nav-link {:padding ".25rem .5rem"}]
                [:.container-fluid {:padding-right "2px"
                                    :padding-left  "2px"}]))
  (rum/mount (page) js/document.body))

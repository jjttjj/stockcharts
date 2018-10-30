(ns clj-iex.schema)

(def data
  {:stock/batch     {:kw-path      "/stock/:symbol/batch/:range"
                     :path-params  [:symbol :range]
                     :path-parts   ["stock" :symbol "batch"]
                     :query-params [:types :symbols :range :filter]}
   :stock/chart     {:kw-path      "/stock/:symbol/chart/:range"
                     :path-params  [:symbol :range]
                     :path-parts   ["stock" :symbol "chart" :range]
                     :query-params [:chartReset :chartSimplify :chartInterval
                                    :changeFromClose :chartLast]}
   :stock/dividends {:kw-path      "/stock/:symbol/dividends/:range"
                     :path-params  [:symbol :range]
                     :path-parts   ["stock" :symbol "dividends" :range]
                     :query-params []}
   :stock/threshold-securities
   {:kw-path      "/stock/:symbol/threshold-securities/:range"
    :path-params  [:symbol :range]
    :path-parts   ["stock" :symbol "threshold-securities" :range]
    :query-params [:format :token]}
   
   :stock/short-interest {:kw-path      "/stock/:symbol/short-interest/:range"
                          :path-params  [:symbol :range]
                          :path-parts   ["stock" :symbol "short-interest" :range]
                          :query-params [:format :token]}
   :stock.news/last      {:kw-path      "/stock/:symbol/news/last/:range"
                          :path-params  [:symbol :range]
                          :path-parts   ["stock" :symbol "news" "last" :range]
                          :query-params []}
   :stock/splits         {:kw-path      "/stock/:symbol/splits/:range"
                          :path-params  [:symbol :range]
                          :path-parts   ["stock" :symbol "splits" :range]
                          :query-params []}
   :ref-data.daily-list/dividends
   {:kw-path      "/ref-data/daily-list/dividends/:range"
    :path-params  [:range]
    :path-parts   ["ref-data" "daily-list" "dividends" :range]
    :query-params [:format :token]}
   
   :ref-data.daily-list/next-day-ex-date
   {:kw-path      "/ref-data/daily-list/next-day-ex-date/:range"
    :path-params  [:range]
    :path-parts   ["ref-data" "daily-list" "next-day-ex-date" :range]
    :query-params [:format :token]}
   
   :ref-data.daily-list/symbol-directory
   {:kw-path      "/ref-data/daily-list/symbol-directory/:range"
    :path-params  [:range]
    :path-parts   ["ref-data" "daily-list" "symbol-directory" :range]
    :query-params [:format :token]}
   
   :deep                   {:kw-path      "/deep"
                            :path-params  []
                            :path-parts   ["deep"]
                            :query-params [:symbols]}
   :deep/auction           {:kw-path      "/deep/auction"
                            :path-params  []
                            :path-parts   ["deep" "auction"]
                            :query-params [:symbols]}
   :deep/book              {:kw-path      "/deep/book"
                            :path-params  []
                            :path-parts   ["deep" "book"]
                            :query-params [:symbols]}
   :deep/official-price    {:kw-path      "/deep/official-price"
                            :path-params  []
                            :path-parts   ["deep" "official-price"]
                            :query-params [:symbols]}
   :deep/op-halt-status    {:kw-path      "/deep/op-halt-status"
                            :path-params  []
                            :path-parts   ["deep" "op-halt-status"]
                            :query-params [:symbols]}
   :deep/security-event    {:kw-path      "/deep/security-event"
                            :path-params  []
                            :path-parts   ["deep" "security-event"]
                            :query-params [:symbols]}
   :deep/ssr-status        {:kw-path      "/deep/ssr-status"
                            :path-params  []
                            :path-parts   ["deep" "ssr-status"]
                            :query-params [:symbols]}
   :deep/system-event      {:kw-path      "/deep/system-event"
                            :path-params  []
                            :path-parts   ["deep" "system-event"]
                            :query-params []}
   :deep/trade-breaks      {:kw-path      "/deep/trade-breaks"
                            :path-params  []
                            :path-parts   ["deep" "trade-breaks"]
                            :query-params [:symbols :last]}
   :deep/trades            {:kw-path      "/deep/trades"
                            :path-params  []
                            :path-parts   ["deep" "trades"]
                            :query-params [:symbols :last]}
   :deep/trading-status    {:kw-path      "/deep/trading-status"
                            :path-params  []
                            :path-parts   ["deep" "trading-status"]
                            :query-params [:symbols]}
   :hist                   {:kw-path      "/hist"
                            :path-params  []
                            :path-parts   ["hist"]
                            :query-params [:date]}
   :tops/last              {:kw-path      "/tops/last"
                            :path-params  []
                            :path-parts   ["tops" "last"]
                            :query-params [:symbols :format]}
   :market                 {:kw-path      "/market"
                            :path-params  []
                            :path-parts   ["market"]
                            :query-params [:format]}
   :ref-data/symbols       {:kw-path      "/ref-data/symbols"
                            :path-params  []
                            :path-parts   ["ref-data" "symbols"]
                            :query-params [:format]}
   :stats.historical/daily {:kw-path      "/stats/historical/daily"
                            :path-params  []
                            :path-parts   ["stats" "historical" "daily"]
                            :query-params [:date :last :format]}
   :stats/historical       {:kw-path      "/stats/historical"
                            :path-params  []
                            :path-parts   ["stats" "historical"]
                            :query-params [:date :format]}
   :stats/intraday         {:kw-path      "/stats/intraday"
                            :path-params  []
                            :path-parts   ["stats" "intraday"]
                            :query-params []}
   :stats/recent           {:kw-path      "/stats/recent"
                            :path-params  []
                            :path-parts   ["stats" "recent"]
                            :query-params []}
   :stats/records          {:kw-path      "/stats/records"
                            :path-params  []
                            :path-parts   ["stats" "records"]
                            :query-params []}
   :stock.market/list      {:kw-path      "/stock/market/list/:listname"
                            :path-params  [:listname]
                            :path-parts   ["stock" "market" "list" :listname]
                            :query-params [:displayPercent]}
   :stock/book             {:kw-path      "/stock/:symbol/book"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "book"]
                            :query-params []}
   :stock/company          {:kw-path      "/stock/:symbol/company"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "company"]
                            :query-params []}
   :stock/delayed-quote    {:kw-path      "/stock/:symbol/delayed-quote"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "delayed-quote"]
                            :query-params []}
   :stock/earnings         {:kw-path      "/stock/:symbol/earnings"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "earnings"]
                            :query-params []}
   :stock/effective-spread {:kw-path      "/stock/:symbol/effective-spread"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "effective-spread"]
                            :query-params []}
   :stock/financials       {:kw-path      "/stock/:symbol/financials"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "financials"]
                            :query-params []}
   :stock/largest-trades   {:kw-path      "/stock/:symbol/largest-trades"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "largest-trades"]
                            :query-params []}
   :stock/logo             {:kw-path      "/stock/:symbol/logo"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "logo"]
                            :query-params []}
   :stock/ohlc             {:kw-path      "/stock/:symbol/ohlc"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "ohlc"]
                            :query-params []}
   :stock/peers            {:kw-path      "/stock/:symbol/peers"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "peers"]
                            :query-params []}
   :stock/previous         {:kw-path      "/stock/:symbol/previous"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "previous"]
                            :query-params []}
   :stock/price            {:kw-path      "/stock/:symbol/price"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "price"]
                            :query-params []}
   :stock/quote            {:kw-path      "/stock/:symbol/quote"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "quote"]
                            :query-params [:displayPercent]}
   :stock/relevant         {:kw-path      "/stock/:symbol/relevant"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "relevant"]
                            :query-params []}
   :stock/stats            {:kw-path      "/stock/:symbol/stats"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "stats"]
                            :query-params []}
   :stock/time-series      {:kw-path      "/stock/:symbol/time-series"
                            :path-params  [:symbol]
                            :path-parts   ["stock" :symbol "time-series"]
                            :query-params []}
   :tops                   {:kw-path      "/tops"
                            :path-params  []
                            :path-parts   ["tops"]
                            :query-params [:symbols :format]}})

(ns clj-iex.query
  #?(:clj
     (:require
      ;;[httpurr.client :as http] 
      ;;[clj-iex.mypurr :as http]
      [ajax.core :as http]
      [clojure.string :as str]
      [jsonista.core :as j]
      
      [clj-iex.schema :refer [data]])
     :cljs 
     (:require
      [ajax.core :as http]
      [clojure.string :as str]
      [clj-iex.schema :refer [data]])))

(def base "https://api.iextrading.com/1.0")

(defn insert-args [segs args]
  (->> segs
       (map #(if (keyword? %) (get args %) %))
       (str/join "/")
       (str "/")))

#?(:clj (def kw-mapper (j/object-mapper {:encode-key-fn name :decode-key-fn keyword})))

(defn json-str->edn [json]
  #?(:clj (j/read-value json kw-mapper)
     :cljs (js->clj (.parse js/JSON json) :keywordize-keys true)))

(defn default-handler [response] (-> response :body))

(defn q
  ([cmd args]
   (q cmd args identity))
  ([cmd args cb]
   (let [{:keys [path-parts query-params]} (or (get data cmd)
                                               (throw (ex-info (str "Invalid command: " cmd)
                                                               {:command cmd
                                                                :args args})))
         endpoint                        (str base (insert-args path-parts args))
         qparams                         (select-keys args query-params)]
     (http/GET endpoint {:params  qparams
                         :handler cb
                         :response-format :text}))))


;;todo :body params
#_(defn ->req [cmd args]
  (let [{:keys [path-parts query-params ]} (or (get data cmd)
                                               (throw (ex-info (str "Invalid command: " cmd)
                                                               {:command cmd
                                                                :args    args})))
        endpoint                        (str base (insert-args path-parts args))]
    )
  )




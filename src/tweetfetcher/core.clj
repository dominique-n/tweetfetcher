(ns tweetfetcher.core
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful :refer [search-tweets statuses-user-timeline]]
    )
  (:import
    (twitter.callbacks.protocols SyncSingleCallback))
  (:require [cheshire.core :refer :all]
            [clj-time.core :as t]
            [clj-time.format :as f]
            ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;helpers

(def custom-time-formatter (f/with-locale (f/formatter  "E MMM dd H:mm:ss Z yyyy")
                        java.util.Locale/ENGLISH))

(defn parse-time [created_at] 
  (f/parse custom-time-formatter created_at))

(defn flatten1d [xs]
  (for [k xs, kk k]
    kk))

(defn slowdown [dn dt xs]
  "count (dn) per seconds (dt) "
  (map #(do (-> (/ dt dn) (* 1000) Math/ceil Thread/sleep)
            %)
       xs))

(defn get-maxid [batch]
  (if (seq batch)
    (->> batch (map :id) (apply min) dec)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;fetch data

(def my-creds  (make-oauth-creds "KgKKVTkrs7yG9tUrAQ4Yjw"
                                 "lqhPEfxK4bENdL0wz7h5GE0OR3EVlRUdOKl8zjvhI"
                                 "470176907-ZlU0Hv3u5B43fs1CRMEYYrBHcjMLDLMFhm3RxGj1"
                                 "dgW3znO1U9B6ak8eRHnHfgYTpL0xIddpNyBmuZJR3gY"))

(def params {:q ""
             :count 100
             :max-id nil
             :since-id nil})

(defn lazy-fetch
  ([tp dn dt params] (flatten1d
                       (slowdown dn dt
                                 (case tp
                                   :search-tweets (lazy-fetch search-tweets :statuses params)
                                   :statuses-user-timeline (lazy-fetch statuses-user-timeline identity params))))) 
  ([fn-fetch fn-extract-batch params] 
   (let [fetch (partial fn-fetch :oauth-creds my-creds :params)]
     (->> params
          fetch :body fn-extract-batch
          (iterate #(if-let [max-id (get-maxid %)]
                      (->> max-id 
                           (assoc params :max-id)
                           fetch :body fn-extract-batch)))
          (take-while seq)))))


;;;
(defn datebound-search [body]
  (-> body :statuses
      first;;check the first because would miss within range if comparing against `last`
      :created_at))

(defn datebound-timeline [tweets]
  (-> tweets first :created_at ))

(defn take-while-recent [fn-datebound tafter xjss]
  (let [tbound #(f/parse custom-time-formatter (fn-datebound %))]
    (take-while #(t/after? (tbound %) tafter)
                xjss)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;process data

(defn get-field 
  ([xjs tp f field] (f (get-field xjs tp field)))
  ([xjs tp field] (case tp
                      :val (get-in xjs field)
                      :count (-> (get-in xjs field) count)
                      :all (->> (drop-last 1 field) (get-in xjs)
                                (map #(get % (last field)))
                                (clojure.string/join ",")))))

(defn get-fields [xjs op-fields]
  ;(map #(get-field xjs (first %) (second %))
  (map #(apply (partial get-field xjs) %)
       op-fields))

(defn make-headers [fields]
  (letfn [(kws2ss [field] 
            (map #(-> % name clojure.string/lower-case) 
                 field))]
    (->> fields
       (map kws2ss)
       (map #(clojure.string/join "X" %)))))

(defn stream-fields 
  ([op-fields xjss] (stream-fields :seq op-fields xjss))
  ([ret-tp op-fields xjss]
   (let [headers (->> op-fields (map last) make-headers)
         hash-fields (fn [fields] (into {} 
                                        (map #(vector %1 %2)
                                             (map keyword headers)
                                             fields)))
         fieldss (map #(get-fields % op-fields) xjss)]
     (case ret-tp
       :seq (cons headers
                  (map #(vector %1 %2) fieldss xjss))
       :map (map #(vector %1 %2)
                 (map hash-fields fieldss)
                 xjss)))))

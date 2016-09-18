(ns tweetfetcher.download.batch1
  (:use
    ;[twitter.oauth]
    ;[twitter.callbacks]
    ;[twitter.callbacks.handlers]
    [twitter.api.restful]
    )
  (:import
    (twitter.callbacks.protocols SyncSingleCallback))
  (:require [tweetfetcher.core :refer :all]
            [tweetfetcher.helpers.jdbc :as jdbch]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.jdbc]
            ))


(def pooled-db (jdbch/pool-db "clintontrump"))

(defn create-table
 ([table] (create-table pooled-db table)) 
 ([db table] (jdbch/create-table db table
                           [:pid :serial "PRIMARY KEY"]
                           [:timestamp :timestamptz "DEFAULT" "NOW()"]
                           [:created_at :timestamptz ]
                           [:id :bigint]
                           [:screen_name "VARCHAR(255)"]
                           [:tweet :text])))

(def primaries-start (t/date-time 2015 02 01))
(def screen-names ["realDonaldTrump" "HillaryClinton" "nytimes" "washingtonpost"])
(def queries ["clinton trump"])

;;create table in db
;(create-table :characters)
;(create-table :anon)


(defn fetch-timeline [screen-name]
  (let [params {:screen-name screen-name :count 200 :include-rts 1}
        op-fields [[:val parse-time [:created_at]] [:val [:id]]]]
    (->> (lazy-fetch :statuses-user-timeline 180 (* 15 60) params)
         (stream-fields :map op-fields)
         (map (fn [[fields tweet]] (assoc fields :tweet (generate-string tweet) :screen_name screen-name)))
         )))

(defn timeline2db 
  ([timeline] (timeline2db pooled-db timeline))
  ([db timeline] (doseq [row timeline]
                   (jdbch/insert! db :characters row))))

(defn fetch-timeline2db [db screen-name]
  (->> screen-name fetch-timeline (timeline2db db)))

(defn -main [tp & screen-names]
  (do (println tp screen-names)
      (case tp
        "start" (doseq [screen-name screen-names]
                  (println screen-name)
                  (fetch-timeline2db pooled-db screen-name))
        "next")))

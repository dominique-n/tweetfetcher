(ns tweetfetcher.download.batch1-test
  (:require [midje.sweet :refer :all]
            [tweetfetcher.download.batch1 :refer :all]
            [tweetfetcher.core :refer :all]
            [tweetfetcher.helpers.jdbc :as jdbch]
            [cheshire.core :refer [generate-string]]
            ;[clj-time.core :as t]
            ;[clj-time.format :as f]
            ;[clj-time.local :as l]
            [clj-time.jdbc]
            ))


(against-background
  [(before :facts (do (def twt {:screen_name "joe"
                                :created_at "Wed Aug 29 17:12:58 +0000 2014"
                                :id 1
                                :text "lol"})
                      (def batch-hill [{:screen_name "hill"
                                        :created_at (parse-time "Wed Aug 29 17:12:58 +0000 2014")
                                        :id 1
                                        :tweet "lol"}
                                       {:screen_name "hill"
                                        :created_at (parse-time "Wed Aug 28 17:12:58 +0000 2014")
                                        :id 2
                                        :tweet "troll"}]) 
                      (def test-db (jdbch/pool-db "test"))))
   (before :facts (do (create-table test-db :anon)
                       (create-table test-db :characters)))
   (after :facts (do (jdbch/drop-table test-db :anon)
                      (jdbch/drop-table test-db :characters)))
   (lazy-fetch & anything) => [twt]]

  (facts "About `fetch-timeline`"
         (fetch-timeline "joe" ) => anything
         (-> (fetch-timeline "joe" ) first) => (contains {:created_at #(instance? org.joda.time.DateTime %)
                                                          :screen_name (:screen_name twt)
                                                          :id (:id twt) 
                                                          :tweet (generate-string twt)}))
  
  (facts "About `timeline2db`"
         (timeline2db test-db batch-hill) => anything
         (-> (jdbch/query test-db "select count(*) from characters") first :count) => 2
         )

  (facts "About `fetch-timeline2db"
         (fetch-timeline2db test-db "joe") => anything
         (-> (jdbch/query test-db "select count(*) from characters") first :count) => 1
         )
  )

(ns tweetfetcher.core-test
  (:require [midje.sweet :refer :all]
            [tweetfetcher.core :refer :all]
            [tweetfetcher.helpers.jdbc :as jdbch]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [twitter.api.restful]
            ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;helpers

(facts "About `parse-time`"
       (parse-time "Wed Aug 29 17:12:58 +0000 2014") => #(instance? org.joda.time.DateTime %)
       )

(facts "About `flatten1d`"
       (flatten1d [[1 2] [3]]) => [1 2 3]
       )

(facts  "About `get-maxid`"
       (get-maxid [{:id 1} {:id 2}]) => 0
       (get-maxid []) => nil
       )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;db

(facts "About `db/helpers`"
       (jdbch/db-spec) => {:classname  "org.postgresql.Driver"
                         :subprotocol  "postgresql"
                         :subname  "//localhost:5432/user"
                         :init-pool-size 4
                         :max-pool-size 50
                         :partitions 2}
       (jdbch/db-spec {:subname "//localhost:5432/any"
                     :max-pool-size 200}) => {:classname  "org.postgresql.Driver"
                                              :subprotocol  "postgresql"
                                              :subname  "//localhost:5432/any"
                                              :init-pool-size 4
                                              :max-pool-size 200
                                              :partitions 2}

       (jdbch/insert! pooled-db :blog_posts
                    {:title  "My first post!" :body  "This is going to be good!"}
                    ) => anything

       (doall (jdbch/query pooled-db "select * from blog_posts")) =>
       [{:body "This is going to be good!", :title "My first post!", :id 1}]

       (jdbch/query pooled-db first "select * from blog_posts") => [[:id 1]]

       (against-background 
         [(before :facts (do (def pooled-db (jdbch/pool-db "test"))
                             (jdbch/create-table pooled-db
                                               :blog_posts
                                               [:id :serial  "PRIMARY KEY"]
                                               [:title  "varchar(255)"  "NOT NULL"]
                                               [:body :text])))
          (after :facts (do (jdbch/drop-table pooled-db :blog_posts)
                            ;(jdbch/closedb pooled-db)
                            ))]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;fetch data

(let [body20 {:body {:statuses [{:id 2}]}}
      body21 {:body {:statuses []}}
      body10 {:body {:statuses []}}]
  (facts "About `lazy-fetch` search-tweets"
         (count (take 2 (lazy-fetch :search-tweets 100 1 {:q "one"}))) => 0
         (lazy-fetch :search-tweets 100 1 {:q "two" :max-id 3}) => (just [{:id 2}])
         (against-background
           (#'twitter.api.restful/search-tweets :oauth-creds my-creds :params {:q "one"}) => body10
           (#'twitter.api.restful/search-tweets :oauth-creds my-creds :params {:q "two" :max-id 3}) =>  body20
           (#'twitter.api.restful/search-tweets :oauth-creds my-creds :params {:q "two" :max-id 1}) => body21)
         ))


(facts "About time and date"
       (let [tbound-in (t/date-time 2013 03 13)
             tbound-out (t/date-time 2015 01 01)
             batches [{:statuses [{:created_at "Wed Aug 29 17:12:58 +0000 2014"}
                                  {:created_at "Wed Aug 29 17:12:58 +0000 2013"}
                                  {:created_at "Wed Aug 29 17:12:58 +0000 2012"}]}]
             timelines [[{:created_at "Wed Aug 29 17:12:58 +0000 2013"}
                         {:created_at "Wed Aug 29 17:12:58 +0000 2012"}]]]

         (take-while-recent datebound-search tbound-in batches) => batches
         (take-while-recent datebound-search tbound-out batches) => empty?

         (take-while-recent datebound-timeline tbound-in timelines) => timelines
         (take-while-recent datebound-timeline tbound-out timelines) => empty?
         ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;process

(facts "About `get-field`"
       (let [twt {:text "tweet" 
                  :user {:id 1} 
                  :entities {:urls [] 
                             :hashtags [{:text "hashtag0"} {:text "hashtag1"}]}}]
         (get-field twt :val [:text]) => "tweet"
         (get-field twt :all [:entities :hashtags :text]) => "hashtag0,hashtag1"
         (get-field twt :count [:entities :urls]) => 0
         (get-field twt :val clojure.string/upper-case [:text]) => "TWEET"
         ))

(facts "About `get-fields`"
       (let [twt {:text "tweet" 
                  :user {:id 1} 
                  :entities {:urls [] 
                             :hashtags [{:text "hashtag0"} {:text "hashtag1"}]}}
             op-val [:val [:text]]
             op-fval [:val clojure.string/upper-case [:text]]
             op-all [:all [:entities :hashtags :text]]
             op-count [:count [:entities :urls]]]
         (get-fields twt [op-val op-all op-count]) => ["tweet" "hashtag0,hashtag1" 0]
         (get-fields twt [op-fval op-all op-count]) => ["TWEET" "hashtag0,hashtag1" 0]
         ))

(facts "About `make-headers`"
       (let [op-val [:val [:text]]
             op-all [:all [:entities :hashtags :text]]
             op-count [:count [:entities :urls]]]
         (make-headers (map second [op-val op-all op-count])) => ["text" "entitiesXhashtagsXtext" "entitiesXurls"]))

(facts "About `stream-fields` as :seq"
       (let [twt {:text "tweet" 
                  :user {:id 1} 
                  :entities {:urls [] 
                             :hashtags [{:text "hashtag0"} {:text "hashtag1"}]}}
             twts (repeat 2 twt)
             op-val [:val [:text]]
             op-all [:all [:entities :hashtags :text]]
             op-count [:count [:entities :urls]]
             op-fields [op-val op-all op-count]]
         (first (stream-fields op-fields twts)) => ["text" "entitiesXhashtagsXtext" "entitiesXurls"]
         (-> (stream-fields op-fields twts) second first) => ["tweet" "hashtag0,hashtag1" 0]
         (-> (stream-fields op-fields twts) second second) => twt
         ))

(facts "About `stream-fields` as :map"
       (let [twt {:text "tweet" 
                  :user {:id 1} 
                  :entities {:urls [] 
                             :hashtags [{:text "hashtag0"} {:text "hashtag1"}]}}
             twts (repeat 2 twt)
             op-val [:val [:text]]
             op-all [:all [:entities :hashtags :text]]
             op-count [:count [:entities :urls]]
             op-fields [op-val op-all op-count]
             twt-hashed0 { :text "tweet" :entitiesXhashtagsXtext "hashtag0,hashtag1" :entitiesXurls 0}]
         (-> (stream-fields :map op-fields twts) first first) => twt-hashed0
         (-> (stream-fields :map op-fields twts) first second) => twt
         
         (-> (stream-fields :map op-fields twts) second first) => twt-hashed0
         (-> (stream-fields :map op-fields twts) second second) => twt
         (-> (stream-fields :map op-fields twts) second second) => twt
         (count (stream-fields :map op-fields twts)) => 2
         ))
       

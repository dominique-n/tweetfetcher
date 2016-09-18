(ns tweetfetcher.process.db0
  (:require [tweetfetcher.core :refer :all]
            [clojure.java.jdbc :as jdbc]
            [java-jdbc.ddl :as ddl]
            [java-jdbc.sql :as sql]))

;(defn -main []
  ;(let [pooled-db-spec  (pooled-datasource (db-spec {:max-pool-size 5}))]

    ;(jdbc/db-do-commands pooled-db-spec false
                         ;(ddl/create-table
                           ;:blog_posts
                           ;[:id :serial  "PRIMARY KEY"]
                           ;[:title  "varchar(255)"  "NOT NULL"]
                           ;[:body :text]))

    ;(jdbc/insert! pooled-db-spec
                  ;:blog_posts
                  ;{:title  "My first post!" :body  "This is going to be good!"})
    ;;; ->  ({:body "This is going to be good!", :title "My first post!", :id 1})

    ;(jdbc/query pooled-db-spec
                ;(sql/select * :blog_posts  (sql/where {:title  "My first post!"})))
    ;;; -> ({:body "This is going to be good!", :title "My first post!", :id 1}))

    ;(.close  (:datasource pooled-db-spec))))

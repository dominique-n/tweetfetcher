(ns tweetfetcher.helpers.jdbc
  (:require [clojure.java.jdbc :as jdbc]
            [java-jdbc.ddl :as ddl]
            [java-jdbc.sql :as sql])
  )

(import 'com.jolbox.bonecp.BoneCPDataSource)

(defn db-spec 
  ([] (db-spec {}))
  ([params]  (reduce (fn [acc [k v]] (assoc acc k v))
                     {
                      :classname  "org.postgresql.Driver"
                      :subprotocol  "postgresql"
                      :subname  "//localhost:5432/dom"
                      :init-pool-size 4
                      :max-pool-size 50
                      :partitions 2
                      }
                     params)))

(defn pooled-datasource  [db-spec]
  (let  [{:keys  [classname subprotocol subname user password
                  init-pool-size max-pool-size idle-time partitions]} db-spec
         min-connections  (inc  (quot init-pool-size partitions))
         max-connections  (inc  (quot max-pool-size partitions))
         cpds  (doto  (BoneCPDataSource.)
                 (.setDriverClass classname)
                 (.setJdbcUrl  (str  "jdbc:" subprotocol  ":" subname))
                 (.setUsername user)
                 (.setPassword password)
                 (.setMinConnectionsPerPartition min-connections)
                 (.setMaxConnectionsPerPartition max-connections)
                 (.setPartitionCount partitions)
                 (.setStatisticsEnabled true)
                 (.setIdleMaxAgeInMinutes  (or idle-time 60)))]
    {:datasource cpds}))

(defn pool-db [dbname] 
  (pooled-datasource
    (db-spec {:subname (str "//localhost:5432/" dbname)})))

(defn create-table [pooled-db table & cols] 
  (jdbc/db-do-commands pooled-db false
                       (jdbc/create-table-ddl table cols 
                              ;{:table-spec "IF NOT EXISTS"}
                              )))

(defn drop-table [pooled-db table]
  (jdbc/db-do-commands pooled-db false
                       (ddl/drop-table table)))

(defn closedb [dbref]
  (.close (:datasource dbref)))


(defn insert! [pooled-db table data]
  (jdbc/insert! pooled-db
                table
                data))

(defn query
  ([pooled-db q] (query pooled-db identity q))
  ([pooled-db f q]
   (jdbc/query pooled-db q
               {:row-fn f})))

(ns tweetfetcher.process.process0
  (:require [tweetfetcher.core :refer :all]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            ))


(def op-fields [[:val [:id]] [:count [:entities :urls]] [:all [:entities :hashtags :text]]
                [:val [:created_at]] [:val [:text]] [:val [:retweet_count]]
                [:val [:user :id]] [:val [:user :followers_count]] [:val [:user :location]]])

(defn -main []
  (with-open [wrt (io/writer "data/process0.txt")]
    (do (io/.write wrt (->> op-fields (map second) 
                            make-headers vector csv/write-csv))
        (with-open [rdr (io/reader "data/batch0.txt")]
          (doseq [batch (line-seq rdr), twt (-> batch (json/parse-string true) :statuses)]
            (io/.write wrt
                       (->> (get-fields twt op-fields) (map str)
                            vector csv/write-csv)))))))

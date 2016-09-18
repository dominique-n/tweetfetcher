(ns tweetfetcher.download.batch0
  (:require [tweetfetcher.core :refer :all]
            [cheshire.core :refer [generate-string]]))

;(defn append [f s] (spit f s :append true))
;(defn mappend [ss] (doseq [s ss] (append "data/down0.txt" (str s "\n"))))
;(->> {:q "clinton"} lazy-fetch (take 2) (map #(generate-string %  {:pretty true})) mappend)
;(->> {:q "clinton"} lazy-fetch (take 2) (map #(generate-string %  {:pretty false})) mappend)
;(def b0 (-> (lazy-fetch "clinton") first doall))
;(println (doall b0))
;(println t2)


(defn append [f s] (spit f s :append true))

(defn mappend [ss] (doseq [s ss] (append "data/batch0.txt" (str s "\n"))))


(defn -main []
  (->> {:q "clinton"} lazy-fetch (take 180)
       (map generate-string)
       mappend))

;(def b0 (->> {:q "clinton"} lazy-fetch (take 2)))
;(println b0)
;(->> b0 (map generate-string ) mappend)

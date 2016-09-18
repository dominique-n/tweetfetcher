(defproject tweetfetcher "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [twitter-api  "0.7.8"]
                 [cheshire  "5.6.1"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.postgresql/postgresql "9.4-1206-jdbc42"] 
                 [com.jolbox/bonecp  "0.8.0.RELEASE"]
                 [org.clojure/java.jdbc  "0.6.1"]
                 [java-jdbc/dsl  "0.1.0"] 
                 [org.slf4j/slf4j-nop "1.7.21"] 
                 [clj-time  "0.11.0"]
                 ]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]}
             ;; You can add dependencies that apply to `lein midje` below.
             ;; An example would be changing the logging destination for test runs.
             :midje {}})
             ;; Note that Midje itself is in the `dev` profile to support
             ;; running autotest in the repl.

  

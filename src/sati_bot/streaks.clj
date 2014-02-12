(ns sati-bot.streaks
  (:require [sati-bot.reddit :as reddit]
            [clojure.edn :as edn]
            [clj-time.core :as time])
  (:import [org.joda.time DateTime]
           [java.util Date]))

#_(def *request* (-> "sati_bot"
    reddit/submissions
    reddit/request
    reddit/submissions-listing
    ))


(defrecord streak [^long streak ^long max last-seen])

(defn- seen-recently?
  [arg]
  arg
  )

(defn load-streaks-file
  "Loads an edn file with streaks data."
  []
  (let[raw (edn/read-string (slurp "resources/streaks.edn"))]
    (into {} (filter seen-recently? raw)) ;; Remove users who aren't participating any more.
    ))
#_
(load-streaks-file)

#_
(let[streak (streak. 1 1 (.toDate (time/now)))
     streaks {"lobsang_ludd" streak}]
  (with-open[writer (clojure.java.io/writer "resources/streaks.edn")]
    (.write writer (pr-str streaks))
    ))

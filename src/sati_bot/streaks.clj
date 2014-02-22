(ns sati-bot.streaks
  (:require [sati-bot.reddit :as reddit]
            [clojure.edn :as edn]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce])
  (:import [org.joda.time DateTime]
           [java.util Date]))

#_(def *request* (-> "sati_bot"
    reddit/submissions
    reddit/request
    reddit/submissions-listing
    ))


(defrecord streak [^long streak ^long max ^DateTime last-seen])

(defn- seen-recently?
  [arg]
  (time/before? (-> 6 time/months time/ago) (-> arg second :last-seen) ))

(defn load-streaks-file
  "Loads (and filters for age) an EDN file with streaks data. "
  []
  (let[raw (edn/read-string {:readers
                             ;;streak needs its own reader because DateTime lacks one.
                             ;;store as java.util.Date which default reader supports, reinflate using clj-time.coerce
                             {'sati_bot.streaks.streak (comp map->streak #(update-in % [:last-seen] coerce/from-date))}}
                            (slurp "resources/streaks.edn"))]
    (into {} (filter seen-recently? raw)) ;; Remove users who aren't participating any more.
    ))


(load-streaks-file)

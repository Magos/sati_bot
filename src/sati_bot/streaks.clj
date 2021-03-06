(ns sati-bot.streaks
  (:require [sati-bot.reddit :as reddit]
            [clojure.edn :as edn]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.string :as string])
  (:import [org.joda.time DateTime]))

(defrecord streak [^long streak ^long max ^DateTime last-seen])

(defn- seen-recently?
  "Last seen within 6 months?"
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


(defn- update
  [streaks [author timestamp]]
  (if-let
    [{:keys [last-seen streak] :as entry} (get streaks author)]
    (let[interval (time/interval last-seen timestamp)
         streaklength (if (< 20 (time/in-hours interval) 56) (inc streak) 1)]
      (when (time/after? timestamp last-seen)
        [author (->streak streaklength (max (:max entry) streaklength) timestamp)]))
    [author (->streak 1 1 timestamp)]
    ))


(defn update-streaks
  "Update a streaks map based on a collection of names/timestamp pairs."
  [streaks comments]
  (into streaks (map (partial update streaks) comments)))


(defn save-streaks-file [streaks]
  (let[deflatable (into {} (map #(update-in % [1 :last-seen] coerce/to-date) streaks) )
       deflated (pr-str deflatable)]
    (spit "resources/streaks.edn" deflated)))

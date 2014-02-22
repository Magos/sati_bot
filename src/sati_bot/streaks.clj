(ns sati-bot.streaks
  (:require [sati-bot.reddit :as reddit]
            [clojure.edn :as edn]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.string :as string])
  (:import [org.joda.time DateTime]))

#_(def *request* (-> "sati_bot"
    reddit/submissions
    reddit/request
    reddit/submissions-listing
    ))


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

(defn- markdown-table
  "Create Markdown for a table displaying a collection
  Columns defines the columns to show - give pairs of accessor sequence and display names."
  [columns coll]
  (let[columnsdef (str "|" (string/join "|" (concat (map second columns)
                                                    "\n"
                                                    ;;All columns are center aligned, for now.
                                                    (map (constantly ":--:") columns)))
                       "\n")
       accessors (for [[x _] columns] (apply comp (reverse x))) ;;Reverse so composition is leftmost-first
       row-fn (apply juxt accessors)
       rows (map row-fn coll)
       formatted (flatten (interpose "\n" rows))
       ](string/join "|" (cons  columnsdef formatted))))

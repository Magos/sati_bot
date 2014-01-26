(ns sati-bot.core
  (:require [clj-time.core :as time]
            [clojure.edn :as edn]
            [sati-bot.reddit :as reddit])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit])
  (:gen-class)
  )

(def ^:dynamic *target-time-zone* (time/time-zone-for-offset +12))
(def ^:dynamic *target-subreddit* "dailypractice")

(defn load-credentials
  "Get login-credentials from an EDN file."
  ([] (load-credentials "resources/credentials.edn"))
  ([filename]
   (edn/read-string (slurp filename))))

(defn- weekday-name "Display names for the week-days." [date]
  (case (time/day-of-week date)
    1 "Monday"
    2 "Tuesday"
    3 "Wednesday"
    4 "Thursday"
    5 "Friday"
    6 "Saturday"
    7 "Sunday"
    "Error"
    ))

(defn- month-name "Display names for the months." [date]
  (case (time/month date)
    1 "January"
    2 "February"
    3 "March"
    4 "April"
    5 "May"
    6 "June"
    7 "July"
    8 "August"
    9 "September"
    10 "October"
    11 "November"
    12 "December"
    13 "Undecimber"
    "Error"
    ))

(defn- ending "Find the number ending to make 1st, 2nd, 3rd..."[number]
  (case number
    11 "th"
    12 "th"
    13 "th"
    (case (mod number 10)
      1 "st"
      2 "nd"
      3 "rd"
      "th")))

(defn checkin-title [date]
  (let[day-of-month (time/day date)
       ending (ending day-of-month)]
    (str "Daily Check-in: " (weekday-name date) ", " (month-name date) " " day-of-month ending)))


(defn make-checkin
  "Make a checkin post request map for a given day, defaulting to the call-time."
  ([](make-checkin (time/now)))
  ([date]
   (let[adjusted (time/to-time-zone date *target-time-zone*)
        title (checkin-title adjusted)

        ]
     (reddit/submit title *target-subreddit*))))


(defn post-checkin
  "Perform a daily check-in:
  * Log in.
  * Check we haven't posted today's message already, in case of reboots or similar.
  * Submit the post."
  []
  (let[session-data (reddit/do-login (load-credentials))
       the-time (time/to-time-zone (time/now) *target-time-zone*) ;;Get today's time
       title (checkin-title the-time) ;;Get today's title.
       last-posts (reddit/get-submissions "sati_bot")
       used-titles (into #{} (map (comp :title :data ) last-posts)) ;;Get the last few titles posted.
       ]
    (if (contains? used-titles title) ;;If we've posted today
      nil ;; then no-op.
      (reddit/request (make-checkin) session-data) ;;Else actually make the post.
      )))

(defn- start-of-day
  "Get the start of the given day."
  [datetime]
  (let[next-day (time/plus datetime (time/days 1))
       start (time/date-time (time/year next-day) (time/month next-day) (time/day next-day) 0 1)]
    (time/from-time-zone start *target-time-zone*)))

(defn -main
  "Schedule the running of check-in jobs."
  [& args]
  (let[executor (ScheduledThreadPoolExecutor. 1)
       first-start-date (time/plus (time/to-time-zone (time/now) *target-time-zone*) (time/days 1))
       start-time (start-of-day first-start-date)
       initial-delay (long (time/in-seconds (time/interval (time/now) start-time)))
       schedule-fn (fn [] (.scheduleAtFixedRate executor post-checkin 0 1 TimeUnit/DAYS))
       ;;Do post-checkin 0 days from now and every 1 days after
       ;;Exploit that Clojure fns implement Runnable and can run directly in Executors.

       ]
    (.schedule executor ^Runnable schedule-fn initial-delay TimeUnit/SECONDS)))

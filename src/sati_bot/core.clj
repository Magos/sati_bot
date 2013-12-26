(ns sati-bot.core
  (:require [clj-time.core :as time]
            [clojure.edn :as edn])
  )

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







(ns sati-bot.reddit
  (:use [sati-bot.core])
  (:require [clj-time.core])
  (:import [java.net URL HttpURLConnection URLEncoder])
  )
#_
(let[r (URL. "http://www.reddit.com/r/dailypractice/hot.xml")
     conn (.openConnection r)]

  (.setRequestMethod conn "GET")
  (.connect conn)
  (.getInputStream conn)

  )
(defn- post-content
  "Takes a map, returns an HTTP POST content filling in a form with the given keys and values"
  [input]
  (let[pairs (map (fn [[name value]] (str name "=" (URLEncoder/encode value "UTF-8"))) input)]
    (apply str (butlast (interleave pairs (repeat "&"))))
    ))

(defn request "Prepare a request to reddit. "
  [{:keys [username password]}]
  (let[login-URL (URL. "http://www.reddit.com/api/login")
       contents (post-content{"api-type" "xml" "passwd" password "user" username})
       conn (.openConnection login-URL)
       ]
    (.setRequestMethod conn "POST")
    (.setDoOutput conn true)
    (spit (.getOutputStream conn) contents)
    (.connect conn)
    (slurp (.getContent conn))))


;(connection {:username "sati_bot" :password "Oe9crTdw1d1A5rNwcvyk"})

(let
  [test {"api-type" "json" "kind" "self" "sr" "dailypractice" "title" (checkin-title (clj-time.core/now))}]
  (post-content test)
  )



















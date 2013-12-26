(ns sati-bot.reddit
  (:use [sati-bot.core])
  (:require [clj-time.core]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.net URL HttpURLConnection URLEncoder])
  )

(def reddit-api "http://www.reddit.com/api/")

(defn- post-content
  "Takes a map, returns an HTTP POST content filling in a form with the given keys and values"
  [input]
  (let[pairs (map (fn [[name value]] (str name "=" (URLEncoder/encode (str value) "UTF-8"))) input)]
    (apply str (butlast (interleave pairs (repeat "&"))))
    ))


(defn request "Prepare a request to reddit, based on a request map.
  This is a pure function and does no IO - call .connect on the returned URLConnection object to do that."
  [{:keys [URL method data cookie modhash]}]
  (let[URL (if (instance? java.net.URL URL) URL (URL. URL))
       conn (.openConnection URL)
       ]
    (.setRequestMethod conn method)
    (.setRequestProperty conn "User-Agent" "sati_bot/0.1.0 by lobsang_ludd")
    (when cookie (.setRequestProperty "Cookie" cookie))
    (when modhash (.setRequestProperty "X-Modhash" modhash))
    (when (and (= "POST" method) data)
      (.setDoOutput conn true)
      (spit (.getOutputStream conn) (post-content data))
      )
    conn))

(defn login
  "Make a request map for logging in as the user with given credentials.
  This returns cookies and modhashes identifying the user account and session, which some calls require."
  [{:keys [username password] :as credentials}]
  {:URL (str reddit-api "login")
   :method "POST"
   :data {"api_type" "json"
          "user" username
          "passwd" password}}
  )

(def me "A request map for the 'me' call."
  {:method "GET"
   :URL (str reddit-api "me.json")
   })

(defn get-session-data
  "Get a map of the user and session data out of a login request.
  These must be included with API calls that 'write' to reddit. Will trigger sending/connecting if not already done."
  [request]
  (let[cookie (str (get (.getHeaderFields request) "Set-Cookie"))
       content (-> (.getInputStream request) io/reader (json/read :key-fn keyword))
       modhash (get-in content [:json :data :modhash])
       ]
    {:cookie cookie :modhash modhash}))


(let[test (login (load-credentials))
     req (request test)]
  (get-session-data req)
  )



















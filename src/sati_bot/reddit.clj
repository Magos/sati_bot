(ns sati-bot.reddit
  (:require [sati-bot.core :as core]
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
  (let[URL (if (instance? java.net.URL URL) URL (URL. (str reddit-api URL))) ;Coerce to java.net.URL if not already an instance.
       conn (.openConnection URL)
       ]
    (.setRequestMethod conn method)
    (.setRequestProperty conn "User-Agent" "sati_bot/0.1.0 by lobsang_ludd")
    (when cookie (.setRequestProperty conn "Cookie" cookie))
    (when modhash (.setRequestProperty conn "X-Modhash" modhash))
    (when (and (= "POST" method) data)
      (.setDoOutput conn true)
      (spit (.getOutputStream conn) (post-content data))
      )
    conn))

(defn login
  "Make a request map for logging in as the user with given credentials.
  This call returns cookies and modhashes identifying the user account and session, which some calls require."
  [{:keys [username password] :as credentials}]
  {:URL "login"
   :method "POST"
   :data {"api_type" "json"
          "user" username
          "passwd" password}}
  )

(def me "A request map for the 'me' call."
  {:method "GET"
   :URL "me.json"
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

(defn do-login
  "Convenience function: Logs in with the given credentials, reads the returned message and makes a session map.
  Merge this into subsequent request maps to identify the user and session."
  [{:keys [username password] :as credentials}]
  (-> credentials
      login
      request
      get-session-data))

(defn submit
  "Make a request map for submitting new content to reddit.
  Takes a title, target subreddit and keyword/value pair options.

  * :link true or :kind 'link' make a link post, otherwise defaults to kind 'text'
  * :URL must accompany a link post and is the URL pointed to.
  * :text is optional and if provided will be the (Markdown formatted) body of the text post.
  * :resubmit true asks reddit to resubmit the link in the case that it's already posted to the subreddit."
  [title subreddit & options]
  (let[{:keys [kind link URL text resubmit] } (apply hash-map options)
       kind (if (string? kind) kind (if link "link" "text"))
       resubmit (if resubmit true false)]
    {:URL (str reddit-api "submit")
     :method "POST"
     :data {"title" title
            "sr" subreddit
            "kind" kind
            "text" text
            "url" URL
            "resubmit" resubmit
            "then" "comments"}
     }))


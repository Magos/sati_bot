(ns sati-bot.reddit
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [clj-time.coerce :as coerce])
  (:import [java.net URL HttpURLConnection URLEncoder])
  )

(def reddit "http://www.reddit.com/")
(def reddit-api (str reddit "api/"))

(def url-encoded "application/x-www-form-urlencoded")

(defn- post-content
  "Takes a map, returns an HTTP POST body filling in a form with the given keys and values"
  [input]
  (let[pairs (map (fn [[name value]] (str name "=" (URLEncoder/encode (if (nil? value) "" (str value)) "UTF-8") )) input)]
    (apply str (butlast (interleave pairs (repeat "&"))))
    ))


(defn request "Send a request to reddit."
  ([requestmap]
   (client/request
    (merge-with conj requestmap {:headers {"User-Agent" "sati_bot/0.1.0 by lobsang_ludd"}})
    )
   )
  ([requestmap session-data]
   (request (merge-with conj requestmap session-data)))
  )

(defn login
  "Make a request map for logging in as the user with given credentials.
  This call returns cookies and modhashes identifying the user account and session, which some calls require."
  [{:keys [username password] :as credentials}]
  {:url (str reddit-api "login")
   :method :post
   :content-type url-encoded
   :body (post-content {"api_type" "json"
                        "user" username
                        "passwd" password})}
  )



(def me "A request map for the 'me' call."
  {:method :get
   :url (str reddit-api "me.json")
   })

(defn get-session-data
  "Get a map of the user and session data out of a login response.
  These must be included with API calls that 'write' to reddit."
  [{:keys [cookies body] :as response}]
  (let[json (json/read-str (:body response) :key-fn keyword)
       errors (get-in json [:json :errors])
       modhash (get-in json [:json :data :modhash])
       ]
    {:cookies cookies :headers {"X-Modhash" modhash}}))

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

  * :link true or :kind 'link' make a link post, otherwise defaults to kind 'self'
  * :URL must accompany a link post and is the URL pointed to.
  * :text is optional and if provided will be the (Markdown formatted) body of a self post.
  * :resubmit true asks reddit to resubmit the link in the case that it's already posted to the subreddit."
  [title subreddit & options]
  (let[{:keys [kind link URL text resubmit] } options
       kind (if (string? kind) kind (if link "link" "self"))
       resubmit (if resubmit true false)]
    {:url (str reddit-api "submit")
     :method :post
     :content-type url-encoded
     :body (post-content {"title" title
                            "sr" subreddit
                            "kind" kind
                            "text" (if text text "")
                            "url" URL
                            "resubmit" resubmit
                            "then" "comments"
                            "api-type" "json"})
     }))


(defn submissions
  "API call to get the most recent submissions of a given user"
  [username]
  {:url (str reddit "user/" username "/submitted.json"
             )
   :method :get
   :content-type url-encoded
   :body (post-content {"show" "given"
                        "sort" "new"
                        "t" "all"
                        "username" username
                        "limit" 10
                        })


   })

(defn submissions-listing
  "Get a vector of submissions out of a 'submitted' API response."
  [response]
  (let[json (json/read-str (:body response) :key-fn keyword)]
    (get-in json [:data :children])))

(defn get-submissions
  "Convenience function: Get a 'submitted' listing for the newest posts by a given username and return a vector of the posts."
  [username]
  (-> username
      submissions
      request
      submissions-listing))

(defn comments
  "Request map for finding the comments on a link or self post.
  Note: article should be the ID36 value defined by reddit on posting."
  [subreddit article]
  {:method :get
   :url (str reddit "r/" subreddit "/comments/" article ".json")
   :body (post-content {"article" article
                        "sort" "top"})
   }
  )

(defn get-commenters
  "Get a seq of comment author/timestamp pairs from a comments listing."
  [listing]
  (->> (json/read-str (:body listing) :key-fn keyword )
       second
       :data
       :children
       (map (comp (juxt :author :created_utc) :data))
       (map #(update-in % [1] (comp coerce/from-long (partial * 1000) long))) ;;Reddit timestamps in seconds, not millis.
       ))


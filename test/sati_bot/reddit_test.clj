(ns sati-bot.reddit-test
  (:use clojure.test
        sati-bot.reddit
        sati-bot.core))



(deftest login-test
  (let[request (request (login (load-credentials "resources/credentials.edn")))
       session-data (get-session-data request)]
    (testing "Connection objects are built appropriately."
      (is (instance? java.net.URLConnection request))
      (is (= "sati_bot/0.1.0 by lobsang_ludd" (.getRequestProperty request "User-Agent")))
      (is (= "www.reddit.com" (-> request .getURL .getHost)))
      (is (= "/api/login" (-> request .getURL .getPath))))
    (testing "API login call returns reasonable values"
      (is (map? session-data))
      (doseq [key [:cookie :modhash]]
        (is (contains? session-data key))
        (is ((complement nil?) (get session-data key))))
      )))

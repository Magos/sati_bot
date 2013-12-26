(ns sati-bot.core-test
  (:use clojure.test
        sati-bot.core))

(deftest credentials-test
  (let[creds1 (load-credentials)
       creds2 (load-credentials "resources/credentials.edn")]
    (doseq [creds [creds1 creds2]]
      (testing "Credentials load."
        (is (not (nil? creds))))
      (testing "Credentials are a map."
        (is (map? creds)))
      (testing "Appropriate keys in credentials?"
        (is (contains? creds :username))
        (is (contains? creds :password)))
      )))


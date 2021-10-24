(ns greenhouse.create-user-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [greenhouse.util :as util]))

(def test-data (atom {}))
(defn- post
  [body]
  (http/post "http://localhost:3000/account"
             {:content-type :json
              :throw-exceptions false
              :body (json/generate-string body)
              :as :json}))

(deftest create-account
  (testing "bad body - name is a number"
    (let [{:keys [status body]} (post {:name 1})]
      (is (= status 400))
      (is (= body "Bad Request"))))
  (testing "create Mr. Black's account"
    (let [{:keys [status body] :as response} (post {:name "Mr. Black"})]
      (util/dbg body)
      (is (= status 200))
      (swap! test-data assoc
             :account-id
             (:account-id body)))))

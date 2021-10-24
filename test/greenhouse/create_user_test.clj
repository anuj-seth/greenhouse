(ns greenhouse.create-user-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [greenhouse.util :as util]))

(def test-data (atom {}))

(deftest create-account
  (let [post (fn [body]
               (http/post "http://localhost:3000/account"
                          {:content-type :json
                           :throw-exceptions false
                           :body (json/generate-string body)
                           :as :json}))]
    (testing "bad body - name is a number"
      (let [{:keys [status body]} (post {:name 1})]
        (is (= status 400))
        (is (= body "Bad Request"))))
    (testing "create Mr. Black's account"
      (let [{:keys [status body] :as response} (post {:name "Mr. Black"})
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= name "Mr. Black"))
        (is (zero? balance))
        (is (int? account-number))
        (swap! test-data assoc
               :account-id
               account-number)))
    (testing "view Mr. Black's account"
      (let [account-id (:account-id @test-data)
            url (str "http://localhost:3000/account/" account-id)
            {:keys [status body] :as response} (http/get url
                                                         {:throw-exceptions false
                                                          :as :json})
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= name "Mr. Black"))
        (is (zero? balance))
        (is (= account-id account-number))))
    (testing "view non-existent account"
      (let [account-id 12345678
            url (str "http://localhost:3000/account/" account-id)
            {:keys [status body] :as response} (http/get url
                                                         {:throw-exceptions false
                                                          :as :json})]
        (is (= status 400))
        (is (= body "Account does not exist"))))))

(deftest deposit
  (let [post (fn [account-id]
               (let [url (str "http://localhost:3000/account/" account-id "/deposit")]
                 (http/post url
                            {:content-type :json
                             :throw-exceptions false
                             :as :json})))]
    (testing "deposit to an account that does not exist"
      (let [{:keys [status body]} (post 123456)]
        (is (= status 400))
        (is (= body "Bad Request")))))
  )

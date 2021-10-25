(ns greenhouse.integration-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [greenhouse.util :as util]))

(def test-data (atom {}))

(deftest check-account-operations
  (let [http-params {:content-type :json
                     :throw-exceptions false
                     :as :json}]
    (testing "create Mr. Black's account"
      (let [req-body (json/generate-string {:name "Mr. Black"})
            {:keys [status body] :as response} (http/post "http://localhost:3000/account"
                                                          (assoc http-params
                                                                 :body req-body))
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
    (testing "deposit 100 to Mr. Black's account"
      (let [req-body (json/generate-string {:amount 100})
            account-id (:account-id @test-data)
            url (str "http://localhost:3000/account/" account-id "/deposit")
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= name "Mr. Black"))
        (is (= account-number account-id))
        (is (zero? (compare balance 100)))))
    (testing "withdraw 50 from Mr. Black's account"
      (let [req-body (json/generate-string {:amount 50})
            account-id (:account-id @test-data)
            url (str "http://localhost:3000/account/" account-id "/withdraw")
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= name "Mr. Black"))
        (is (= account-number account-id))
        (is (zero? (compare balance 50)))))
    (testing "withdraw more than balance from Mr. Black's account"
      (let [req-body (json/generate-string {:amount 200})
            account-id (:account-id @test-data)
            url (str "http://localhost:3000/account/" account-id "/withdraw")
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))
            {:keys [account-number name balance]} body]
        (is (= status 400))
        (is (= body "Not enough balance"))))
    (testing "create Mr. White's account"
      (let [req-body (json/generate-string {:name "Mr. White"})
            {:keys [status body] :as response} (http/post "http://localhost:3000/account"
                                                          (assoc http-params
                                                                 :body req-body))
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= name "Mr. White"))
        (is (zero? balance))
        (is (int? account-number))
        (swap! test-data assoc
               :recipient-account-id
               account-number)))
    (testing "send more money than Mr. Black has to Mr. White"
      (let [mr-blacks-account (:account-id @test-data)
            mr-whites-account (:recipient-account-id @test-data)
            url (str "http://localhost:3000/account/" mr-blacks-account "/send")
            req-body (json/generate-string {:account-number mr-whites-account
                                            :amount 100})
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))]
        (is (= status 400))
        (is (= body "Not enough balance"))))
    (testing "send 25 from Mr. Black to Mr. White"
      (let [mr-blacks-account (:account-id @test-data)
            mr-whites-account (:recipient-account-id @test-data)
            url (str "http://localhost:3000/account/" mr-blacks-account "/send")
            req-body (json/generate-string {:account-number mr-whites-account
                                            :amount 25})
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= account-number mr-blacks-account))
        (is (= name "Mr. Black"))
        (is (zero? (compare balance 25)))))
    (testing "send 10 from Mr. White to Mr. Black"
      (let [mr-blacks-account (:account-id @test-data)
            mr-whites-account (:recipient-account-id @test-data)
            url (str "http://localhost:3000/account/" mr-whites-account "/send")
            req-body (json/generate-string {:account-number mr-blacks-account
                                            :amount 10})
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))
            {:keys [account-number name balance]} body]
        (is (= status 200))
        (is (= account-number mr-whites-account))
        (is (= name "Mr. White"))
        (is (zero? (compare balance 15)))))
    (testing "fetch Mr. Black's transaction log"
      (let [mr-blacks-account (:account-id @test-data)
            mr-whites-account (:recipient-account-id @test-data)
            url (str "http://localhost:3000/account/" mr-blacks-account "/audit")
            {:keys [status body]} (http/get url
                                            http-params)]
        (is (= status 200))
        (is (= body [{:sequence 3 :description (str "receive from #" mr-whites-account) :credit 10.0}
                     {:sequence 2 :description (str "send to #" mr-whites-account) :debit 25.0}
                     {:sequence 1 :description "withdraw" :debit 50.0}
                     {:sequence 0, :description "deposit" :credit 100.0}]))))))

(deftest check-exception-scenarios
  (let [http-params {:content-type :json
                     :throw-exceptions false
                     :as :json}]
    (testing "bad body while creating an account - name is a number"
      (let [req-body (json/generate-string {:name 1})
            {:keys [status body]} (http/post "http://localhost:3000/account"
                                             (assoc http-params
                                                    :body req-body))]
        (is (= status 400))
        (is (= body "Bad Request - request validation failed"))))
    (testing "view non-existent account"
      (let [account-id 12345678
            url (str "http://localhost:3000/account/" account-id)
            {:keys [status body] :as response} (http/get url
                                                         http-params)]
        (is (= status 400))
        (is (= body "Account does not exist"))))
    (testing "deposit to an account that does not exist"
      (let [account-id 123456
            url (str "http://localhost:3000/account/" account-id "/deposit")
            req-body (json/generate-string {:amount 100})
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))]
        (is (= status 400))
        (is (= body "Account does not exist"))))
    (testing "withdraw from an account that does not exist"
      (let [account-id 123456
            url (str "http://localhost:3000/account/" account-id "/withdraw")
            req-body (json/generate-string {:amount 100})
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))]
        (is (= status 400))
        (is (= body "Account does not exist"))))
    (testing "transfer money from an account to itself"
      (let [account-id 123456
            url (str "http://localhost:3000/account/" account-id "/send")
            req-body (json/generate-string {:account-number account-id
                                            :amount 100})
            {:keys [status body]} (http/post url
                                             (assoc http-params
                                                    :body req-body))]
        (is (= status 400))
        (is (= body "Sender and recipient cannot be same"))))))

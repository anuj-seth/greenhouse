(ns greenhouse.create-user-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [greenhouse.util :as util]))

(def test-data (atom {}))

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
        (is (= body "Account does not exist"))))))

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
        (util/dbg body)
        (is (= status 200))
        (is (= name "Mr. Black"))
        (is (= account-number account-id))
        (is (zero? (compare balance 100)))))))

;; (let [req-body (json/generate-string {:amount 100})
;;       account-id (:account-id @test-data)
;;       url (str "http://localhost:3000/account/" 1 "/withdraw")
;;       {:keys [status body]} (http/post url
;;                                        (assoc {:content-type :json
;;                                                :throw-exceptions false
;;                                                :as :json}
;;                                               :body req-body))
;;       {:keys [account-number name balance]} body]
;;   (util/dbg body)
;;   (is (= status 200))
;;   (is (= name "Mr. Black"))
;;   (is (= account-number account-id))
;;   (is (zero? (compare balance 100))))

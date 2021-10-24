(ns greenhouse.api
  (:require [clojure.java.jdbc :as jdbc]
            [greenhouse.sql.accounts :as accounts]
            [greenhouse.util :as util]))

(defmacro in-transaction
  [db f request-data]
  `(jdbc/with-db-transaction [tx# ~db]
     (~f {:db tx#
          :data ~request-data})))

(defn create-account
  [{:keys [db data]}]
  (util/dbg data)
  (let [account-name (get-in data
                             [:account :name])
        {:keys [account-id]} (accounts/insert-account db
                                                      {:account-name account-name})]
    (accounts/open-balance db
                           {:account-id account-id
                            :amount 0})
    {:status :ok
     :data (update data
                   :account
                   assoc
                   :id account-id
                   :balance 0)}))

(comment
  (let [db-spec {:dbtype "postgres"
                 :host "localhost",
                 :port "5432"
                 :user "greendev",
                 :password "green"
                 :dbname "greenhouse"}]
    (jdbc/with-db-transaction [tx db-spec]
      (accounts/insert-account tx
                               {:account-name "abc"})
      (jdbc/db-set-rollback-only! tx)
      "abcd"
      ))

  )


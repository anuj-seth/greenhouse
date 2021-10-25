(ns greenhouse.api
  (:require [clojure.java.jdbc :as jdbc]
            [greenhouse.api-helpers :as helpers]
            [greenhouse.sql.accounts :as accounts]
            [greenhouse.util :as util]))

(defn in-transaction
  [db f request-data]
  (jdbc/with-db-transaction [tx db]
    (let [[params err] (f {:db tx
                           :data request-data})]
      (if (nil? err)
        {:status :ok
         :data (:data params)}
        (do
          (jdbc/db-set-rollback-only! tx)
          {:status :failure
           :error-msg err})))))

(defn bind-error [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro until-err->> [val & fns]
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))

(defn create-account
  [params]
  (until-err->> params
                helpers/insert-account
                helpers/open-balance))

(defn view-account
  [{:keys [db data] :as params}]
  (let [account-id (get-in data
                           [:account :id])
        {:keys [balance-amount account-name]} (accounts/view-account-and-balance db
                                                                                 {:account-id account-id})]
    (if (or (nil? balance-amount)
            (nil? account-name))
      [params "Account does not exist"]
      (let [updated-params (update-in params
                                      [:data :account]
                                      assoc
                                      :balance balance-amount
                                      :name account-name)]
        [updated-params nil]))))

(defn deposit
  [params]
  (until-err->> params
                (partial helpers/check-and-maybe-lock-account
                         :credit-account
                         false)
                (partial helpers/add-transaction
                         :credit-account
                         "credit")))

(defn deposit-and-return-balance
  [params]
  (until-err->> params
                deposit
                (partial helpers/fetch-balance-account-info
                         :credit-account)))

(defn withdraw
  [params]
  (until-err->> params
                (partial helpers/check-and-maybe-lock-account
                         :debit-account
                         true)
                (partial helpers/fetch-balance-account-info
                         :debit-account)
                helpers/check-sufficient-balance
                (partial helpers/add-transaction
                         :debit-account
                         "debit")))

(defn withdraw-and-return-balance
  [params]
  (until-err->> params
                withdraw
                (partial helpers/fetch-balance-account-info
                         :debit-account)))

(defn transfer
  [params]
  (util/dbg params)
  (until-err->> params
                helpers/sender-and-recipient-not-same
                withdraw
                deposit
                (partial helpers/fetch-balance-account-info
                         :debit-account)))

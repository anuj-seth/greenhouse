(ns greenhouse.api
  (:require [clojure.java.jdbc :as jdbc]
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

(defn- insert-account
  [{:keys [db data] :as params}]
  (let [account-name (get-in data
                             [:account :name])
        {:keys [account-id]} (accounts/insert-account db
                                                      {:account-name account-name})
        params-with-account-id (update-in params
                                          [:data :account]
                                          assoc
                                          :id account-id)]
    [params-with-account-id nil]))

(defn- open-balance
  [{:keys [db data] :as params}]
  (let [account-id (get-in data
                           [:account :id])
        {:keys [amount]} (accounts/open-balance db
                                                {:account-id account-id
                                                 :amount 0})
        updated-params (update-in params
                                  [:data :account]
                                  assoc
                                  :balance amount)]
    [updated-params nil]))

(defn create-account
  [params]
  (until-err->> params
                insert-account
                open-balance))

(defn- fetch-balance-with-id
  [db account-id]
  (accounts/view-account-and-balance db
                                     {:account-id account-id}))

(defn- fetch-balance-account-info
  [account-key {:keys [db data] :as params}]
  (let [account-id (get-in data
                           [account-key :id])
        {:keys [balance-amount account-name]} (accounts/view-account-and-balance db
                                                                                 {:account-id account-id})
        updated-params (update-in params
                                  [:data account-key]
                                  assoc
                                  :balance balance-amount
                                  :name account-name)]
    [updated-params nil]))

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

(defn- check-and-maybe-lock-account
  [account-key lock? {:keys [db data] :as params}]
  (let [account-id (get-in data
                           [account-key :id])
        return (accounts/select-and-maybe-lock-account db
                                                       {:account-id account-id
                                                        :lock lock?})]
    (if (= {} return)
      [params "Account does not exist"]
      [params nil])))

(defn- add-transaction
  [account-key trx-type {:keys [db data] :as params}]
  (let [{:keys [id amount]} (data account-key)
        {:keys [transaction-id]} (util/dbg (accounts/insert-transaction db
                                                                        {:account-id id
                                                                         :transaction-type trx-type
                                                                         :amount amount}))
        updated-params (update-in params
                                  [:data account-key]
                                  assoc
                                  :transaction-id transaction-id)]
    [updated-params nil]))

(defn deposit
  [{:keys [db data] :as params}]
  (util/dbg data)
  (until-err->> params
                (partial check-and-maybe-lock-account
                         :credit-account
                         false)
                (partial add-transaction
                         :credit-account
                         "credit")))

(defn deposit-and-return-balance
  [{:keys [db data] :as params}]
  (util/dbg data)
  (until-err->> params
                deposit
                (partial fetch-balance-account-info
                         :credit-account)))

(defn- check-sufficient-balance
  [{:keys [data] :as params}]
  (util/dbg data)
  (let [{:keys [balance amount]} (:debit-account data)
        new-balance (- balance amount)]
    (if (>= new-balance 0)
      [params nil]
      [nil "Not enough balance"])))

(defn withdraw
  [{:keys [db data] :as params}]
  (util/dbg data)
  (until-err->> params
                (partial check-and-maybe-lock-account
                         :debit-account
                         true)
                (partial fetch-balance-account-info
                         :debit-account)
                check-sufficient-balance
                (partial add-transaction
                         :debit-account
                         "debit")))

(defn withdraw-and-return-balance
  [{:keys [db data] :as params}]
  (util/dbg data)
  (until-err->> params
                withdraw
                (partial fetch-balance-account-info
                         :debit-account)))

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


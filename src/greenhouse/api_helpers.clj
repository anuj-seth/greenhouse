(ns greenhouse.api-helpers
  (:require [greenhouse.sql.accounts :as accounts]))

(defn insert-account
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

(defn open-balance
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

(defn fetch-balance-account-info
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

(defn check-and-maybe-lock-account
  [account-key lock? {:keys [db data] :as params}]
  (let [account-id (get-in data
                           [account-key :id])
        return (accounts/select-and-maybe-lock-account db
                                                       {:account-id account-id
                                                        :lock lock?})]
    (if (= {} return)
      [params "Account does not exist"]
      [params nil])))

(defn add-transaction
  [account-key trx-type {:keys [db data] :as params}]
  (let [{:keys [id amount]} (data account-key)
        {:keys [transaction-id]} (accounts/insert-transaction db
                                                              {:account-id id
                                                               :transaction-type trx-type
                                                               :amount amount})
        updated-params (update-in params
                                  [:data account-key]
                                  assoc
                                  :transaction-id transaction-id)]
    [updated-params nil]))

(defn check-sufficient-balance
  [{:keys [data] :as params}]
  (let [{:keys [balance amount]} (:debit-account data)
        new-balance (- balance amount)]
    (if (>= new-balance 0)
      [params nil]
      [nil "Not enough balance"])))

(defn sender-and-recipient-not-same
  [{:keys [data] :as params}]
  (let [{sender :id} (:debit-account data)
        {recipient :id} (:credit-account data)]
    (if (= sender recipient)
      [nil "Sender and recipient cannot be same"]
      [params nil])))

(defn link-debit-and-credit
  [{:keys [data db] :as params}]
  (let [{sender-id :id debit-id :transaction-id} (:debit-account data)
        {receiver-id :id credit-id :transaction-id} (:credit-account data)]
    (accounts/insert-fund-transfer db
                                   {:debit-transaction-id debit-id
                                    :credit-transaction-id credit-id
                                    :sender-id sender-id
                                    :receiver-id receiver-id})
    [params nil]))

(defn select-transactions
  [{:keys [data db] :as params}]
  (let [trxs (accounts/select-transactions db
                                           {:account-id (get-in data
                                                                [:account :id])})
        account-log (map (fn [{:keys [amount transaction-type] :as trx}]
                           (-> trx
                               (assoc (keyword transaction-type) amount)
                               (dissoc :amount
                                       :transaction-type)))
                         trxs)
        updated-params (update params
                               :data
                               assoc :account-log account-log)]
    [updated-params nil]))

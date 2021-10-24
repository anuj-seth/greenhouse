(ns greenhouse.api
  (:require [clojure.java.jdbc :as jdbc]
            [greenhouse.sql.accounts :as accounts]
            [greenhouse.util :as util]))

(defn in-transaction
  [db f request-data]
  (jdbc/with-db-transaction [tx db]
    (let [[params err] (f {:db tx
                           :data request-data})
          response {:data (:data params)}]
      (if (nil? err)
        (assoc response
               :status :ok)
        (do
          (jdbc/db-set-rollback-only! tx)
          (assoc response
                 :status :failure
                 :error-msg err))))))

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
  (view-account params)
  ;; (until-err->> params
                
  ;;               open-balance)
  )

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


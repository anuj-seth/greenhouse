(ns db
  (:require [integrant.repl.state :refer [system]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [ragtime.core :as ragtime]
            [clojure.java.jdbc :as clj-jdbc]))

(defn- migration-config
  [tx]
  {:datastore (jdbc/sql-database tx
                                 {:migrations-table "bank.ragtime_migrations"})
   :migrations (jdbc/load-resources "migrations")})

(defn- run-in-transaction
  [f & args]
  (assert system "system must be set. type (go) ?")
  (println f)
  (clj-jdbc/with-db-transaction [tx (:greenhouse/postgres system)]
    (apply f
           (migration-config tx)
           args)))

(defn migrate-schema
  []
  (run-in-transaction repl/migrate))

(defn rollback-schema
  ([]
   (run-in-transaction repl/rollback))
  ([amount-or-id]
   (run-in-transaction repl/rollback amount-or-id)))

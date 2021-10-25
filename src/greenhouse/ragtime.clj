(ns greenhouse.ragtime
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [integrant.core :as ig]
            [ragtime.core :as ragtime]
            ragtime.jdbc
            [ragtime.strategy]))

(defn migrate
  [{:keys [database path migrations-config]}]
  (jdbc/with-db-transaction [tx database]
    (let [datastore (ragtime.jdbc/sql-database tx
                                               migrations-config)
          migrations (ragtime.jdbc/load-resources path)]
      (ragtime/migrate-all datastore
                           (ragtime/into-index migrations)
                           migrations))))

(defmethod ig/init-key :greenhouse/ragtime
  [_ {:keys [migrate? ragtime-config]}]
  (when migrate?
    (migrate ragtime-config))
  nil)

(ns greenhouse.sql.accounts
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "greenhouse/sql/accounts.sql" {:quoting :ansi})

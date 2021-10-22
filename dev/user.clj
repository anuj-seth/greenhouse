(ns user
  (:require [clojure.tools.namespace.repl :as tools-repl]
            [clojure.edn :as edn]
            [integrant.repl]
            [greenhouse.system :as system]
            ;; always load greenhouse.postgres namespace in 'lein repl' or on cider-jack-in.
            ;; this namespace registers the functions to convert DB table column names from
            ;; snake case to kebab case
            [greenhouse.postgres]))

(tools-repl/set-refresh-dirs "src" "dev")
(integrant.repl/set-prep! system/new-system)

(defn go
  []
  (tools-repl/refresh)
  (integrant.repl/go))

(defn reset
  []
  (integrant.repl/reset))


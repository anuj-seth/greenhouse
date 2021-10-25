(ns greenhouse.core
  (:require [clojure.tools.logging :as logging]
            [integrant.core :as ig]
            [greenhouse.system :as sys])
  (:gen-class))

(def system nil)

(defn -main
  []
  (logging/debug "Starting system")
  (alter-var-root #'system
                  (fn [_]
                    (ig/init (sys/new-system))))
  (logging/info "System started")
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (logging/info "Stopping system")
                               (ig/halt! system)
                               (logging/info "Stopped system"))))
  ;; All threads are daemon, so block forever:
  @(promise))

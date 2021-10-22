(ns greenhouse.system
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]))

(defn- config
  []
  (let [edn-resource (io/resource "config.edn")
        edn-data (slurp edn-resource)]
    (ig/read-string edn-data)))

(defn new-system
  "Construct a new system"
  []
  (let [config (config)]
    (ig/load-namespaces config)
    config))

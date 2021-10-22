(ns greenhouse.postgres
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as logging]
            [hikari-cp.core :as hikari-cp]
            [hugsql.core]
            [hugsql.adapter]
            [camel-snake-kebab.core :as csk]
            [clojure.java.jdbc :as jdbc]
            [java-time]))

(defn- transform-keys
  [f m]
  (letfn [(transform [[k v]] [(f k) v])]
    (into {}
          (map transform
               m))))

(defn result-one-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-one this result options)
       (transform-keys csk/->kebab-case-keyword)))

(defn result-many-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-many this result options)
       (map #(transform-keys csk/->kebab-case-keyword %))))

(defmethod hugsql.core/hugsql-result-fn :1 [_] 'greenhouse.postgres/result-one-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :one [_] 'greenhouse.postgres/result-one-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :* [_] 'greenhouse.postgres/result-many-snake->kebab)
(defmethod hugsql.core/hugsql-result-fn :many [_] 'greenhouse.postgres/result-many-snake->kebab)

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [x _ _]
    (java-time/local-date x))
  java.sql.Timestamp
  (result-set-read-column [x _ _]
    (java.time.OffsetDateTime/ofInstant
      (.toInstant x)
      (java.time.ZoneId/of "UTC"))))

(defmethod ig/init-key :greenhouse/postgres
  [_ config-map]
  (let [config (merge {:auto-commit false
                       :read-only false
                       :connection-timeout 5000
                       :validation-timeout 1000
                       :idle-timeout 600000
                       :max-lifetime 1800000
                       :minimum-idle 10
                       :adapter "postgresql"
                       :leak-detection-threshold 180000
                       :register-mbeans true}
                      config-map)]
    (logging/info "Starting hikari-cp")
    {:datasource (hikari-cp/make-datasource config)}))

(defmethod ig/halt-key! :greenhouse/postgres
  [_ config-map]
  (logging/info "Stopping hikari-cp")
  (hikari-cp/close-datasource (:datasource config-map)))

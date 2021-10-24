(defproject greenhouse "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ragtime "0.8.0"]
                 [integrant "0.8.0"]
                 [hikari-cp "2.13.0"]
                 [org.postgresql/postgresql "42.2.18"]
                 [cheshire "5.10.0"]
                 [com.layerware/hugsql "0.5.1"]
                 [clojure.java-time "0.3.2"]
                 [camel-snake-kebab "0.4.2"]
                 [org.clojure/tools.logging "1.1.0"]
                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [prismatic/schema "1.1.9"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [ring/ring-jetty-adapter "1.9.4"]
                 [ring/ring-defaults "0.3.2"]
                 [clj-http "3.12.3"]]
  :main ^:skip-aot greenhouse.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev" "src" "test"]
                   :dependencies [[org.clojure/tools.namespace "1.1.0"]
                                  [org.clojure/java.jdbc "0.7.11"]
                                  [integrant/repl "0.3.2"]]}})

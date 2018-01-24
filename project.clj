(defproject lambdawerk-backend-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.7.5"]
                 [org.postgresql/postgresql "42.2.0"]
                 [hikari-cp "2.0.1"]
                 [clj-time "0.14.2"]
                 [com.velisco/strgen "0.1.5"]
                 [honeysql "0.9.1"]
                 [nilenso/honeysql-postgres "0.2.3"]
                 [com.stuartsierra/frequencies "0.1.0"]]

  :profiles {:dev     {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                      [org.clojure/test.check "0.9.0"]]
                       :source-paths ["dev"]}
             :uberjar {:aot          :all
                       :jvm-opts     ["-Dclojure.compiler.direct-linking=true"]
                       :main         lambdawerk-backend-test.core}})

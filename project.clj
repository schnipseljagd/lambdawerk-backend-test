(defproject lambdawerk-backend-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.7.5"]]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/test.check "0.9.0"]]
                   :source-paths ["dev"]}})

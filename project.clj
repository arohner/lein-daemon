(defproject lein-daemon "0.5.0-SNAPSHOT"
  :description "A lein plugin that daemonizes a clojure process"
  :url "https://github.com/arohner/leiningen"
  :license {:name "Eclipse Public License"}
  :dependencies [[com.sun.jna/jna "3.0.9"]
                 [org.jruby.ext.posix/jna-posix "1.0.3"]]
  :dev-dependencies [[lein-clojars "0.6.0"]])

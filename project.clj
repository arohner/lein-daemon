(defproject lein-daemon "0.2"
  :dependencies [[commons-daemon "1.0.1"]]
  :dev-dependencies [[org.clojure/clojure "1.2.0"]]
  :aot [leiningen.daemon.daemonProxy])
(defproject lein-daemon "0.2"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [commons-daemon "1.0.1"]]
  :aot [leiningen.daemon.daemonProxy])
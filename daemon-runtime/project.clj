(defproject lein-daemon-runtime "0.5.0"
  :description "Runtime code for lein-daemon"
  :url "https://github.com/arohner/lein-daemon"
  :license {:name "Eclipse Public License"}
  :eval-in-leiningen true
  :dependencies [[com.sun.jna/jna "3.0.9"]
                 [org.jruby.ext.posix/jna-posix "1.0.3"]
                 [me.raynes/conch "0.4.0"]]
  :dev-dependencies [[lein-clojars "0.9.1"]
                     [org.clojure/clojure "1.4.0"]])
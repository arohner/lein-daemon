(defproject lein-daemon "0.5.3"
  :description "A lein plugin that daemonizes a clojure process"
  :url "https://github.com/arohner/leiningen"
  :license {:name "Eclipse Public License"}
  :eval-in-leiningen true
  :profiles {:dev
             {:dependencies
              [[bond "0.2.3" :exclusions [org.clojure/clojure]]]}})

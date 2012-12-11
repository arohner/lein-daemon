(ns leiningen.daemon-starter
  (:require [leiningen.core.eval :as eval]
            [leiningen.daemon.common :as common]))

(defn add-daemon-runtime-dependency
  [project]
  (if (some #(= 'lein-daemon-runtime (first %)) (:dependencies project))
    project
    (update-in project [:dependencies] conj ['lein-daemon-runtime "0.5.0-fcc70a204ed93c0409e6887cef29238c736f989e"])))

(defn daemon-starter [project & [alias daemon-name & args :as all-args]]
  (let [info (get-in project [:daemon alias])
        ns (symbol (:ns info))
        pid-path (common/get-pid-path project alias)
        debug? (common/debug? project alias)]
    (eval/eval-in-project (add-daemon-runtime-dependency project)
                          `(do
                             (leiningen.daemon.runtime/init ~pid-path :debug ~debug?)
                             (let [main-symbol# '~'-main
                                   main# (ns-resolve '~ns main-symbol#)]
                               (when-not main#
                                 (leiningen.daemon.runtime/abort (format "%s/%s not found" '~ns main-symbol#)))
                               (main# ~@args)))
                          `(do
                             (System/setProperty "leiningen.daemon" "true")
                             (require '[leiningen.daemon.runtime])
                             (println "requiring" '~ns)
                             (require '~ns)))))
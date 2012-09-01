(ns leiningen.daemon
  (:use [leiningen.core.eval :only [eval-in-project]]
        [leiningen.core.main :only [abort]]
        [leiningen.help :only [help-for]])
  (:import java.io.File)
  (require [leiningen.daemon-runtime :as runtime]))

(defn wait-for
  "periodically calls test, a fn of no arguments, until it returns
  true, or timeout (in seconds) exceeded. Calls fail, a fn of no
  arguments if test never returns true"
  [test fail timeout]
  (let [start (System/currentTimeMillis)
        end (+ start (* timeout 1000))]
    (while (and (< (System/currentTimeMillis) end) (not (test)))
           (Thread/sleep 1))
    (if (< (System/currentTimeMillis) end)
      true
      (fail))))

(defn get-pid-path [project alias]
  (get-in project [:daemon alias :pidfile]))

(defn pid-present? [project alias]
  (runtime/get-pid (get-pid-path project alias)))

(defn running? [project alias]
  (runtime/process-running? (pid-present? project alias)))

(defn inconsistent?
  "true if pid is present, and process not running"
  [project alias]
  (and (pid-present? project alias) (not (running? project alias))))

(defn start-main
  [project alias & args]
  (let [ns          (get-in project [:daemon alias :ns])
        daemon-args (get-in project [:daemon alias :args] [])
        all-args    (concat daemon-args args)
        timeout     (* 5 60)]
    (if (not (pid-present? project alias))
      (do
        (println "starting" alias)
        (let [main-process (future (eval-in-project project
                                                    `(do
                                                       (System/setProperty "leiningen.daemon" "true")
                                                       (require 'leiningen.daemon-runtime)
                                                       (leiningen.daemon-runtime/init ~(get-pid-path project alias) :debug ~(get-in project [:daemon alias :debug]))
                                                       ((ns-resolve '~(symbol ns) '~'-main) ~@all-args))
                                                    `(do
                                                       (require '~(symbol ns)))))]
          (wait-for #(running? project alias) #(throw (Exception. (format "%s failed to start in %s seconds" alias timeout))) timeout)
          (println "waiting for pid file to appear")
          (println alias "started")
          @main-process))
      (if (running? project alias)
        (do
          (println alias "already running")
          (System/exit 1))
        (do
          (println "not starting, pid file present")
          (System/exit 2))))))

(defn stop [project alias]
  (let [pid (runtime/get-pid (get-pid-path project alias))
        timeout 60]
    (when (running? project alias)
      (println "sending SIGTERM to" pid)
      (runtime/sigterm pid))
    (wait-for #(not (running? project alias)) #(throw (Exception. (format "%s failed to stop in %d seconds" alias timeout))) timeout)
    (-> (get-pid-path project alias) (File.) (.delete))))

(defn check [project alias]
  (when (running? project alias)
    (do (println alias "is running") (System/exit 0)))
  (when (inconsistent? project alias)
    (do (println alias "pid present, but NOT running") (System/exit 2)))
  (do (println alias "is NOT running") (System/exit 1)))

(defn check-valid-daemon [project alias]
  (let [d (get-in project [:daemon alias])
        pid-path (get-in project [:daemon alias :pidfile])]
    (when (not d)
      (abort (str "daemon " alias " not found in :daemon section")))
    (when (not pid-path)
      (abort (str ":pidfile is required in daemon declaration")))
    true))

(declare usage)
(defn ^{:help-arglists '([])} daemon
  "Run a -main function as a daemon, with optional command-line arguments.

In project.clj, define a keyvalue pair that looks like
 :daemon {:foo {:ns foo.bar
                :pidfile \"foo.pid\"}}

USAGE: lein daemon start :foo
USAGE: lein daemon stop :foo
USAGE: lein daemon check :foo

this will apply the -main method in foo.bar.

On the start call, additional arguments will be passed to -main

USAGE: lein daemon start :foo bar baz 
"
  [project & [command daemon-name & args :as all-args]]
  (when (or (nil? command)
            (nil? daemon-name))
    (abort (help-for "daemon")))
  (let [command (keyword command)
        daemon-name (if (keyword? (read-string daemon-name))
                      (read-string daemon-name)
                      daemon-name)]
    (check-valid-daemon project daemon-name)
    (condp = (keyword command)
      :start (apply start-main project daemon-name args)
      :stop (stop project daemon-name)
      :check (check project daemon-name)
      (abort (str command " is not a valid command")))))
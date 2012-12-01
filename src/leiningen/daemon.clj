(ns leiningen.daemon
  (:import java.io.File)
  (:require [leiningen.core.main :refer (abort)]
            [leiningen.core.eval :as eval]
            [leiningen.help :refer (help-for)]
            [leiningen.daemon.common :as common]))

(defmacro with-deps [form init-form]
  `(eval/eval-in-project
    project-dependencies
    ~form
    ~init-form))

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

(defn get-pid
  "read and return the pid number contained in a pid-file, or nil"
  [path]
  (try
    (-> path (slurp) (Integer/parseInt))
    (catch java.io.FileNotFoundException e
      nil)
    ;; this sometimes happens as a race, if the file has been created,
    ;; but the pid write hasn't fsync'd yet.
    (catch java.lang.NumberFormatException e
      nil)))

(defn pid-present? [project alias]
  (get-pid (common/get-pid-path project alias)))

(defn process-running?
  "returns true if the process with the specified PID is running"
  [pid]
  (with-deps
    `(leiningen.daemon.runtime/process-running? ~pid)
    `(require 'leiningen.daemon.runtime)))

(defn running? [project alias]
  {:post [(do (println "running?:" %) true)]}
  (time (process-running? (time (pid-present? project alias)))))

(defn inconsistent?
  "true if pid is present, and process not running"
  [project alias]
  (and (pid-present? project alias) (not (running? project alias))))

(defn spawn [& cmd]
  (with-deps
    `(leiningen.daemon.runtime/spawn ~@cmd)
    `(require '[leiningen.daemon.runtime])))

(defn do-start [project alias]
  (let [timeout (* 5 60)
        trampoline-file (System/getProperty "leiningen.trampoline-file")]
    (println "pid not present, starting")
    ;;(spawn "lein" "daemon-stage2")
    (spit trampoline-file (format "lein daemon-start %s" alias))
    ;; (println "waiting for pid file to appear")
    ;; (wait-for #(running? project alias) #(throwf (format "%s failed to start in %s seconds" alias timeout)) timeout)
    ;; (println alias "started")
    ;; (System/exit 0)
    ))

(defn start-main
  [project alias & args]
  (println "start-main:" alias args)
  (if (not (pid-present? project alias))
    (do-start project alias)
    (if (running? project alias)
      (do
        (println alias "already running")
        (System/exit 1))
      (do
        (println "not starting, pid file present")
        (System/exit 2)))))

(defn sigterm [pid]
  (with-deps
    `(runtime/sigterm ~pid)
    `(require '[leiningen.daemon.runtime :as runtime])))

(defn stop [project alias]
  (let [pid (get-pid (common/get-pid-path project alias))
        timeout 60]
    (when (running? project alias)
      (println "sending SIGTERM to" pid)
      (sigterm pid))
    (wait-for #(not (running? project alias)) #(throwf "%s failed to stop in %d seconds" alias timeout) timeout)
    (-> (common/get-pid-path project alias) (File.) (.delete))))

(defn check [project alias]
  (when (running? project alias)
    (do (println alias "is running") (System/exit 0)))
  (when (inconsistent? project alias)
    (do (println alias "pid present, but NOT running") (System/exit 2)))
  (do (println alias "is NOT running") (System/exit 1)))

(defn check-valid-daemon [project alias]
  (let [d (get-in project [:daemon alias])
        pid-path (common/get-pid-path project alias)]
    (when (not d)
      (abort (str "daemon " alias " not found in :daemon section")))
    (when (not pid-path)
      (abort (str ":pidfile is required in daemon declaration")))
    true))

(defn abort-when-not [expr message & message-args]
  (when-not expr
    (abort (apply format message message-args))))

(declare usage)
(defn ^{:help-arglists '([])} daemon
  "Run a -main function as a daemon, with optional command-line arguments.

In project.clj, add a field pair that looks like
 :daemon {:foo {:ns foo.bar
                :pidfile \"foo.pid\"}}

USAGE: lein daemon start :foo
USAGE: lein daemon stop :foo
USAGE: lein daemon check :foo

this will apply the -main method in foo.bar.

On the start call, any additional arguments will be passed to -main

USAGE: lein daemon start :foo bar baz
"
  [project & [command daemon-name & args :as all-args]]
  (when (or (nil? command)
            (nil? daemon-name))
    (abort (help-for "daemon")))
  (let [command (keyword command)
        daemon-name (if (keyword? (read-string daemon-name))
                      (read-string daemon-name)
                      daemon-name)
        alias (get-in project [:daemon daemon-name])]
    (check-valid-daemon project daemon-name)
    (condp = (keyword command)
      :start (apply start-main project daemon-name args)
      :stop (stop project daemon-name)
      :check (check project daemon-name)
      (abort (str command " is not a valid command")))))
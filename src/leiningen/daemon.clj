(ns leiningen.daemon
  (:import java.io.File)
  (:require [clojure.string :as str]
            [clojure.java.shell :as sh]
            [leiningen.core.main :refer (abort)]
            [leiningen.core.eval :as eval]
            [leiningen.help :refer (help-for)]
            [leiningen.daemon.common :as common]))

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

(defn pid-present?
  "Returns the pid contained in the pidfile, if present, else nil"
  [project alias]
  (get-pid (common/get-pid-path project alias)))

(defn running?
  "True if there's a process running with the pid contained in the pidfile"
  [project alias]
  (common/process-running? (pid-present? project alias)))

(defn inconsistent?
  "true if pid is present, and process not running"
  [project alias]
  (and (pid-present? project alias) (not (running? project alias))))

(defn wait-for-running [project alias & {:keys [timeout]
                                         :or {timeout 300}}]
  (println "waiting for pid file to appear")
  (wait-for #(running? project alias)
            #(common/throwf (format "%s failed to start in %s seconds" alias timeout)) timeout)
  (println alias "started"))

(defn get-lein-script []
  (System/getProperty "leiningen.script"))

(defn do-start [project alias args]
  (let [timeout (* 5 60)
        arg-str (str/join " " args)
        log-file (format "%s.log" alias)
        lein (get-lein-script)
        nohup-cmd (format "nohup %s daemon-starter %s %s </dev/null &> %s &" lein (name alias) arg-str log-file)]
    (println "pid not present, starting")
    (when-not lein
      (abort "lein-daemon requires lein-2.0.0-RC1 or later"))
    (common/sh! "bash" "-c" nohup-cmd)
    (wait-for-running project alias)))

(defn start-main
  [project alias args]
  (let [running? (running? project alias)
        pid-present? (pid-present? project alias)]
    (cond
     running? (abort "already running")
     pid-present? (abort "not starting, pid file present")
     :else (do-start project alias args))))

(defn delete-pid [project alias]
  (-> (common/get-pid-path project alias) (File.) (.delete)))

(defn stop [project alias]
  (let [pid (get-pid (common/get-pid-path project alias))
        timeout 60]
    (when (running? project alias)
      (println "sending SIGTERM to" pid)
      (common/sigterm pid))
    (wait-for #(not (running? project alias)) #(common/throwf "%s failed to stop in %d seconds" alias timeout) timeout)
    (delete-pid project alias)))

(defn check [project alias]
  (when (running? project alias)
    (do (println alias "is running") (System/exit 0)))
  (when (inconsistent? project alias)
    (do (println alias "pid present, but NOT running") (System/exit 2)))
  (do (println alias "is NOT running") (System/exit 1)))

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
        daemon-name (common/get-daemon-name project daemon-name)
        daemon-args (get-in project [:daemon daemon-name :args])
        args (concat daemon-args args)]
    (condp = (keyword command)
      :start (start-main project daemon-name args)
      :stop (stop project daemon-name)
      :check (check project daemon-name)
      (abort (str command " is not a valid command")))))
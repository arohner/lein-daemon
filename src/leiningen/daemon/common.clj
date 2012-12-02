(ns leiningen.daemon.common
  ;; this will be loaded by leiningen.daemon, so it can't have any dependencies not in lein
  (:require [clojure.java.shell :as sh]))

(defn throwf [& message]
  (throw (Exception. (apply format message))))

(defn daemon-info-exists? [project alias]
  (get-in project [:daemon alias]))

(defn get-pid-path [project alias]
  (get-in project [:daemon alias :pidfile]))

(defn debug? [project alias]
  (get-in project [:daemon alias :debug]))

(defn sh! [& args]
  (let [resp (apply sh/sh args)
        exit-code (:exit resp)]
    (when (not (zero? exit-code))
      (printf "%s returned %s: %s\n" args exit-code resp)
      (throwf "%s returned %s" args exit-code))
    resp))

(defn ps [pid]
  (sh/sh "ps" (str pid)))

(defn process-running?
  "returns true if the process with the specified PID is running"
  [pid]
  (-> (ps pid) :exit zero?))

(defn sigterm [pid]
  (sh/sh "kill" "-SIGTERM" (str pid)))
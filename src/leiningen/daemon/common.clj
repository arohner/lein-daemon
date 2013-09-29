(ns leiningen.daemon.common
  ;; this will be loaded by leiningen.daemon, so it can't have any dependencies not in lein
  (:require [clojure.java.shell :as sh]))

(defn throwf [& message]
  (throw (Exception. (apply format message))))

(defn daemon-info-exists? [project daemon-name]
  (get-in project [:daemon daemon-name]))

(defn default-pid-name [daemon-name]
  (format "%s.pid" (name daemon-name)))

(defn default-log-file-name [daemon-name]
  (format "%s.log" (name daemon-name)))

(defn get-pid-path [project daemon-name]
  (get-in project [:daemon daemon-name :pidfile] (default-pid-name daemon-name)))

(defn get-log-file-path [project daemon-name]
  (get-in project [:daemon daemon-name :logfile] (default-log-file-name daemon-name)))

(defn get-daemon-name [project name]
  (cond
   (get-in project [:daemon name]) name
   (get-in project [:daemon (keyword name)]) (keyword name)
   :else (throw (Exception. (str "daemon " name " not found in :daemon section")))))

(defn debug? [project daemon-name]
  (get-in project [:daemon daemon-name :debug]))

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
(ns leiningen.daemon.runtime
  (:import java.io.FileOutputStream
           (org.jruby.ext.posix POSIXFactory
                                POSIXHandler))
  (:require [clojure.java.io :refer (reader)]
            [conch.sh :as sh]
            [conch.core :as conch]))

(defn throwf [& message]
  (throw (Exception. (apply format message))))

(def handler
  (proxy [POSIXHandler]
      []
    (error [error extra]
      (println "error:" error extra))
    (unimplementedError [methodname]
      (throwf "unimplemented method %s" methodname))
    (warn [warn-id message & data]
      (println "warning:" warn-id message data))
    (isVerbose []
      false)
    (getCurrentWorkingDirectory []
      (System/getProperty "user.dir"))
    (getEnv []
      (map str (System/getenv)))
    (getInputStream []
      System/in)
    (getOutputStream []
      System/out)
    (getErrorStream []
      System/err)
    (getPID []
      (rand-int 65536))))

(def C (POSIXFactory/getPOSIX handler true))

(defn closeDescriptors []
  (.close System/out)
  (.close System/err)
  (.close System/in))

(defn is-daemon? []
  (System/getProperty "leiningen.daemon"))

(defn chdirToRoot []
  (.chdir C "/")
  (System/setProperty "user.dir" "/"))

(defn get-current-pid []
  (.getpid C))

(defn write-pid-file
  "Write the pid of the current process to pid-path"
  [pid-path]
  (spit pid-path (str (get-current-pid))))

(defn process-running?
  "returns true if the process with the specified PID is running"
  [pid]
  (sh/with-programs [ps]
    (-> (ps (str pid) {:verbose true})
        :exit-code
        deref
        zero?)))

(defn spawn
  "Takes a seq of strings to be started in a subprocess. Does not wait
  for the subprocess to exit"
  [& cmd]
  (let [args (concat ["nohup"] cmd)
        _ (println "spawn: args=" args)
        {:keys [in out err process] :as proc} (apply conch/proc args)]
    (future (conch/stream-to-out proc :out))
    ;;(.close in)
    ;;(conch/done proc)
    proc))

(defn init
  "do all the post-fork setup. set session id, close file descriptors, write pid file"
  [pid-path & {:keys [debug]}]
  ;; (.setsid C)
  ;; (when (not debug)
  ;;   (closeDescriptors))
  (write-pid-file pid-path))

(defn sigterm [pid]
  (.kill C pid 15))

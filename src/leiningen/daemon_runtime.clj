(ns leiningen.daemon-runtime
  (:import (org.jruby.ext.posix POSIXFactory
                                POSIXHandler))
  (:import java.io.FileOutputStream)
  (:use [clojure.java.shell :only (sh)])
  (:use [clojure.java.io :only (reader)]))

(def handler (proxy [POSIXHandler]
                 []
               (error [error extra]
                 (println "error:" error extra))
               (unimplementedError [methodname]
                 (throw (Exception. (format "unimplemented method %s" methodname))))
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

(defn writePidFile [pid-path]
  (with-open [w (FileOutputStream. pid-path)]
    (.write w (-> (get-current-pid) (str) (.getBytes "UTF-8")))
    (-> w (.getFD) (.sync))))

(defn init
  "do all the post-fork setup. set session id, close file descriptors, chdir to /, write pid file"
  [pid-path & {:keys [debug]}]
  (.setsid C)
  (when (not debug)
    (closeDescriptors))  
  (writePidFile pid-path))

(defn process-running?
  "returns true if the process with the specified PID is running"
  [pid]
  (-> (sh "ps" (str pid))
      :exit
      (= 0)))

(defn sigterm [pid]
  (.kill C pid 15))

(defn get-pid
  "returns the pid for a given pid-file, or nil"
  [path]
  (try
    (with-open [r (reader path)]
      (-> r
          (slurp)
          (Integer/parseInt)))
    (catch java.io.FileNotFoundException e
      nil)))

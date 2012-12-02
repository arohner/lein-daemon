(ns leiningen.daemon.runtime
  (:import java.io.FileOutputStream
           (org.jruby.ext.posix POSIXFactory
                                POSIXHandler))
  (:require [clojure.java.io :refer (reader)]
            [clojure.java.shell :as sh]))

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
  (let [pid (str (get-current-pid))]
    (printf "writing pid %s to %s" pid pid-path)
    (spit pid-path pid)))

(defn init
  "do all the post-fork setup. set session id, close file descriptors, write pid file"
  [pid-path & {:keys [debug]}]
  ;; (.setsid C)
  ;; (when (not debug)
  ;;   (closeDescriptors))
  (write-pid-file pid-path))

(defn sigterm [pid]
  (.kill C pid 15))

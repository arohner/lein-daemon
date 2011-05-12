(ns leiningen.daemon-runtime
  (:import com.sun.akuma.CLibrary)
  (:import com.sun.jna.Native)
  (:use [clojure.java.shell :only (sh)])
  (:use [clojure.java.io :only (reader)]))

(def LIBC (Native/loadLibrary "c" CLibrary))

(defn closeDescriptors []
  (.close System/out)
  (.close System/err)
  (.close System/in))

(defn is-daemon? []
  (System/getProperty "leiningen.daemon"))

(defn chdirToRoot []
  (.chdir LIBC "/")
  (System/setProperty "user.dir" "/"))

(defn get-current-pid []
  (.getpid LIBC))

(defn writePidFile [pid-path]
  (with-open [w (clojure.java.io/writer pid-path)]
    (.write w (str (get-current-pid)))))

(defn init
  "do all the post-fork setup. set session id, close file descriptors, chdir to /, write pid file"
  [pid-path]
  (.setsid LIBC)
  (closeDescriptors)
  (writePidFile pid-path)
  (chdirToRoot))

(defn process-running?
  "returns true if the process with the specified PID is running"
  [pid]
  (-> (sh "ps" (str pid))
      :exit
      (= 0)))

(defn sigterm [pid]
  (.kill LIBC pid 15))

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

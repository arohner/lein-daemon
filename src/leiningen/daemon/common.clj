(ns leiningen.daemon.common)

(def project-dependencies {:dependencies '[[me.raynes/conch "0.4.0"]
                                           [com.sun.jna/jna "3.0.9"]
                                           [org.jruby.ext.posix/jna-posix "1.0.3"]
                                           [lein-daemon-runtime "0.5.0"]]})

(defn throwf [& message]
  (throw (Exception. (apply format message))))

(defn daemon-info-exists? [project alias]
  (get-in project [:daemon alias]))

(defn get-pid-path [project alias]
  (get-in project [:daemon alias :pidfile]))

(defn debug? [project alias]
  (get-in project [:daemon alias :debug]))
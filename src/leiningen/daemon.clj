(ns leiningen.daemon
  (:use [leiningen.compile :only [eval-in-project]]))

(def jsvc-singular-args #{:version :nodetach :debug :check :stop}) ;; jsvc command line args that don't take a parameter

(defn mangle-option [[arg value]]
  (cond
   (contains? jsvc-singular-args arg) [(format "-%s" (name arg))]
   :else [(format "-%s" (name arg)) value]))

(defn start-handler [java daemon-map cmdline-args]
  (doto java
    (.setJvm "jsvc")
    (.clearArgs)
    (.setClassname "leiningen.daemon.daemonProxy"))
  (doseq [option (concat (mapcat mangle-option (:options daemon-map)) ["-Djava.awt.headless=true"])]
    (.. java (createJvmarg) (setValue option)))
  (doseq [arg (concat [(:ns daemon-map)] (:args daemon-map) cmdline-args)]
    (.. java (createArg) (setValue arg)))
  java)

(defn stop-handler [java daemon-map cmdline-args]
  (start-handler java daemon-map cmdline-args)
  (.. java (createJvmarg) (setValue "-stop"))
  java)

(defn check-handler [java daemon-map cmdline-args]
  (start-handler java daemon-map cmdline-args)
  (.. java (createJvmarg) (setValue "-check"))
  java)

;; map of allowed commands to the handler fn
(def handler-map
     {"start" start-handler
      "stop" stop-handler
      "check" check-handler})

(defn print-usage []
  (println "Usage:")
  (println "lein daemon start/stop/check name-of-daemon"))

(defn print-available-daemons [project]
  (println (count (:daemon project)) "available daemon commands")
     (doseq [daemon (keys (:daemon project))]
       (println "   " daemon)))

(defn run-daemon [project handler daemon cmdline-args]
  (let [rc (eval-in-project project nil #(handler % daemon cmdline-args))]
    (when (and (number? rc) (not= 0 rc))
      ;; note that starting can still fail later on, and we have no
      ;; way to detect it.
      (println "FAIL. See the error log for more information"))))

(defn daemon-not-found [project daemon-str]
  (println "Unrecognized daemon:" daemon-str)
  (print-available-daemons project))

(defn unrecognized-command []
  (println "Unrecognized command. Must be one of " (keys handler-map)))

(defn daemon
  "starts a daemon process. daemonname is a key in the :daemon map in project.clj to run."
  ([project cmd daemon-str & cmdline-args]
     (let [handler (get handler-map cmd)
           daemon (get-in project [:daemon daemon-str])]
       (cond
        (and handler daemon) (run-daemon project handler daemon cmdline-args)
        handler (daemon-not-found project daemon-str)
        daemon (unrecognized-command))))
  ([project]
     (print-usage)
     (print-available-daemons project))
  ([project _]
     (print-usage)
     (print-available-daemons project)))
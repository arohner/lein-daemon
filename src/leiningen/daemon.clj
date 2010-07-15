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

(defn daemon
  "starts a daemon process. daemonname is a key in the :daemon map in project.clj to run."
  ([project cmd daemon-str & cmdline-args]
     (let [handler (get handler-map cmd)
           daemon (get-in project [:daemon daemon-str])]
       (cond
        (and handler daemon) (let [rc (eval-in-project project nil #(handler % daemon cmdline-args))]
                               (when-not (= 0 rc)
                                 (println "FAIL. See the error log for more information")))
        handler (do
                  (println "Unrecognized daemon:" daemon-str)
                  (print-available-daemons project))
        daemon (println "Unrecognized command. Must be one of " (keys handler-map)))))
  ([project]
     (print-usage)
     (print-available-daemons project))
  ([project _]
     (print-usage)
     (print-available-daemons project)))
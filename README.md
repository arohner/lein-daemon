Lein-daemon is a lein plugin that starts a clojure process as a daemon. It uses the Apache Commons Daemon library to daemonize. 

To use, add a :daemon option to your project.clj, it looks like

:daemon { "name-of-service" {:class "MyDaemonClass"
                             :args ["foo" "bar"]
                             :options {:errfile "/path/to/file"
                                       :user "bob"}}}

The keys of the daemon map are the names of services that can be started at the command-line, from lein. Start a process with "lein daemon start name-of-service". 

= Class = 
jsvc requires the service to be started with a class that implements the Daemon interface. Compiling classes is a pain, so Lein-daemon takes a clojure namespace with functions that "implement" the daemon interface, i.e. clojure functions (init), (start), (stop), with the same signatures as the Daemon interface. 

The Daemon interface is found at: http://commons.apache.org/daemon/apidocs/org/apache/commons/daemon/Daemon.html

= Arguments =
:args is a list of arguments that will be passed to the class. The arguments are accessible through the DaemonContext argument to init().

= Options =
:options is a map of options passed to jsvc. Keywords will be converted to the java argument format (:foo -> "-foo"). See the list at http://commons.apache.org/daemon/jsvc.html. For the options that don't take an argument, for example -nodetach, the value in the clojure map will be ignored. 

Lein's :jvm-opts and classpath (:source-path, :jar-dir, etc) specified in project.clj will also be passed to jsvc.


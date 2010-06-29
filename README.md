Lein-daemon is a lein plugin that starts a clojure process as a daemon. It uses the Apache Commons Daemon library to daemonize. 

To use, add a :daemon option to your project.clj, it looks like

    :daemon { "name-of-service" {:ns "my.name.space"
                                 :args ["foo" "bar"]
                                 :options {:errfile "/path/to/file"
                                           :user "bob"}}}

The keys of the daemon map are the names of services that can be started at the command-line, from lein. Start a process with "lein daemon start name-of-service". 

Install
=======
Because lein-daemon contains code that lein runs, and code that the started process runs, **you'll need to specify lein-daemon once in :dependencies, and again in :dev-dependencies **

NS
==
jsvc requires the service to be started with a class that implements the Daemon interface. Pre-compiling classes is a pain, so lein-daemon takes a clojure namespace with functions that "implement" the daemon interface, i.e. clojure functions (init [& cmdline-args]), (start []), (stop []) (destroy []). The :ns key specifies a clojure namespace that will be require'd, containing the functions init, start, stop, destroy. All functions except start are optional. 

The Daemon interface is found at: http://commons.apache.org/daemon/apidocs/org/apache/commons/daemon/Daemon.html

Arguments
=========
:args is a list of arguments that will be passed to the init method. The arguments in the daemon map will be concatenated with the arguments on the command line.

Options
=======
:options is a map of options passed to jsvc. Keywords will be converted to the java argument format (:foo -> "-foo"). See the list at http://commons.apache.org/daemon/jsvc.html. For the options that don't take an argument, for example -nodetach, the value in the clojure map will be ignored. 

Lein's :jvm-opts and classpath (:source-path, :jar-dir, etc) specified in project.clj will also be passed to jsvc.

Operation
=========
Lein daemon currently has three commands, start, stop, check. Start with "lein daemon start name-of-service foo bar baz". In this example, foo bar baz are extra arguments that will be passed to the init method. A pid file is written to the location of :pidfile in options (/var/run/jsvc.pid, by default). 

Stop the process with "lein daemon stop name-of-service". Daemon will use the pid file to identify which process to stop.

Limitations / Security / Threads
================================
Lein daemon will add -Djava.awt.headless=true to the list of java options.

If the daemon process is started using sudo or the root user, the init function will be called while the process still holds root privileges. The process will drop to regular user privileges before calling the start function. **Do not spawn new threads during the init process, spawn them during the start function**.

Use the :user option to specify which account to drop to.
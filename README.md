Lein-daemon is a lein plugin that starts a clojure process as a daemon. It uses the Apache Commons Daemon library to daemonize. 

To use, add a :daemon option to your project.clj, it looks like

    :daemon { "name-of-service" {:ns "my.name.space"
                                 :args ["foo" "bar"]
                                 :options {:errfile "/path/to/file"
                                           :user "bob"}}}

The keys of the daemon map are the names of services that can be started at the command-line, from lein. Start a process with "lein daemon start name-of-service". 

Install
=======
add lein-daemon to your leiningen project file, **both as a regular dependency, AND a dev-dependency**::

    :dependencies [[lein-daemon "0.2.1"]]
    :dev-dependencies [[lein-daemon "0.2.1]]

Lein-daemon requires this because it contains code that lein runs, and code that the launched process runs. 

lein daemon also depends on the Apache Commons Daemon library. 

If you're using MacPorts, "sudo port install commons-daemon"
On Ubuntu, "sudo apt-get install jsvc" 

NS
==
jsvc requires the service to be started with a class that implements the Daemon interface. Pre-compiling classes is a pain, so lein-daemon takes a clojure namespace with functions that "implement" the daemon interface, i.e. clojure functions (init [& cmdline-args]), (start []), (stop []) (destroy []). The :ns key specifies a clojure namespace that will be require'd, containing the functions init, start, stop, destroy. All functions except start are optional. 

[More information on the Daemon interface here](http://commons.apache.org/daemon/apidocs/org/apache/commons/daemon/Daemon.html)

Arguments
=========
:args is a list of arguments that will be passed to the init method. Extra arguments on the command line will be appended to the end of args.

Options
=======
:options is a map of options passed to jsvc. Keywords will be converted to the java argument format (:foo -> "-foo"). [See here for the list](http://commons.apache.org/daemon/jsvc.html). For the options that don't take an argument, for example -nodetach, the value in the clojure map will be ignored. 

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

By default, jsvc will write the pid file to /var/run/jsvc.pid, which requires root access on most systems. Either start the daemon with sudo, or override the pidfile location to somewhere that doesn't require root access.

Tips & Tricks
=============
It's sometimes tricky to figure out what is going wrong when the service doesn't start properly. To debug, you can add the ":debug true" and ":nodetach true" to your daemon :options map. debug prints a lot of information on startup, and nodetach prevents the process from exiting, if it would normally exit because nothing is keeping the process alive.

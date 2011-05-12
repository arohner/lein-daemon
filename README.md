Lein-daemon is a lein plugin that starts a clojure process as a daemon. In version 0.3, it has been re-written to not require any code that lein can't download.

To use, add a :daemon option to your project.clj, it looks like

    :daemon { :name-of-service {:ns my.name.space
                                :pidfife "path-to-pidfile.pid"}}

The keys of the daemon map are the names of services that can be started at the command-line, from lein. Start a process with "lein daemon start :name-of-service". 

Install
=======
add lein-daemon to your leiningen project file, as a dev-dependency::

    :dev-dependencies [[lein-daemon "0.3"]]

lein-daemon requires JNA to load the C standard library, so if you're using an uncommon JVM, you might need to install JNA on your box. If you're running OSX or Hotspot, you're probably fine.

NS
==
Like "lein run", "lein daemon" will call a function named -main, in the namespace specified by :ns. Any additional command line arguments will be passed to -main.

Operation
=========
Lein daemon currently has three commands, start, stop, check. Start with "lein daemon start name-of-service foo bar baz". In this example, foo bar baz are extra arguments that will be passed to the -main method. A pid file is written to the location of :pidfile. This path can be relative to the project directory, or absolute. 

Stop the process with "lein daemon stop name-of-service". Daemon will use the  pid file to identify which process to stop.

Check if the process is still running with "lein daemon check name-of-service".

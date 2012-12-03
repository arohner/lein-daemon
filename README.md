Lein-daemon is a lein plugin that starts a clojure process as a daemon.

To use, add a :daemon option to your project.clj, it looks like

```clojure
    :daemon {:name-of-service {:ns my.name.space
                               :pidfile "path-to-pidfile.pid"}}
```

The keys of the daemon map are the names of services that can be started at the command-line. Start a process with "lein daemon start :name-of-service". pidfile specifies where the pid file will be written. This path can be relative to the project directory, or absolute.

If the pidfile is not specified, it defaults to name-of-service.pid, in the project's directory.

Install
=======
add lein-daemon to your leiningen project file, as a plugin::

```clojure
    :plugins [[lein-daemon "0.5.0"]]
```

lein-daemon requires JNA to load the C standard library, so if you're using an uncommon JVM, you might need to install JNA on your box. If you're running Hotspot, you're probably fine.

NS
==
Like `lein run`, `lein daemon` will call a function named `-main`, in the namespace specified by `:ns`. Any additional command line arguments to `lein daemon start foo` will be passed to -main.

Operation
=========
Lein daemon currently has three commands, `start`, `stop`, `check`. Start with `lein daemon start :name-of-service`. This will call the -main function in the specified ns, with no arguments. Extra arguments may also be specified, like `lein daemon start :service foo bar baz`.

Stop the process with `lein daemon stop name-of-service`. Daemon will use the pid file to identify which process to stop.

Check if the process is still running with `lein daemon check name-of-service`.


Limitations / Assumptions
===========

lein-daemon 0.5.0 and above require lein2. lein-daemon 0.4.0 works on lein versions up to 1.6.1.1. There is no version that supports 1.7.x

lein-daemon assumes you're on a unix-like system with `nohup` and `bash` installed.

Currently, your `lein` executable needs to be called `lein`, not `lein2` or anything like that. There's an open PR on leiningen proper to resolve that.

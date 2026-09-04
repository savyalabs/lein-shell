# lein-shell

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.savya/lein-shell.svg)](https://clojars.org/net.clojars.savya/lein-shell)
[![cljdoc](https://cljdoc.org/badge/net.clojars.savya/lein-shell)](https://cljdoc.org/d/net.clojars.savya/lein-shell/CURRENT)
[![test](https://github.com/savyalabs/lein-shell/actions/workflows/test.yml/badge.svg)](https://github.com/savyalabs/lein-shell/actions/workflows/test.yml)

A Leiningen plugin that calls shell commands.

## Stack

<a href="https://clojure.org"><img src="https://img.shields.io/badge/Clojure-5881D8?style=flat&logo=clojure&logoColor=fff" alt="Clojure" /></a>
<a href="https://leiningen.org"><img src="https://img.shields.io/badge/Leiningen-2D2D2D?style=flat" alt="Leiningen" /></a>

## Installation

Put `[net.clojars.savya/lein-shell "1.1.0"]` in the `:plugins` vector of your `:user` profile
in `~/.lein/profiles.clj` to use lein shell for one user.

Put `[net.clojars.savya/lein-shell "1.1.0"]` in the `:plugins` vector of `project.clj` to build this project with lein-shell.
If `project.clj` has no `:plugins` vector, use this form:

```clj
(defproject your-project-here "version"
 ...
 :plugins [[net.clojars.savya/lein-shell "1.1.0"]]
 ...)
```

## Usage

lein-shell calls the shell command with the parameters that you give.
For example, run this command to print a message from Leiningen:

    $ lein shell cowsay 'Hello from Leiningen!'
	 _______________________
    < Hello from Leiningen! >
     -----------------------
            \   ^__^
             \  (oo)\_______
                (__)\       )\/\
                    ||----w |
                    ||     ||

You can omit `lein shell` and get the same result for this command.
Use lein-shell when `make` or `ANTLR` generates files for a Clojure project.
For example, add this to `project.clj` to call `make` before tasks run:

```clj
:prep-tasks [["shell" "make"] "javac" "compile"]
```

If the command returns a nonzero exit code, shell tries to exit Leiningen with the same code.
You can change this behavior and other settings.

## Documentation

See [the documentation][documentation] for more information and examples.

[documentation]: https://github.com/savyalabs/lein-shell/blob/main/doc/DOCUMENTATION.md

## License

Copyright © 2013-2015 Jean Niklas L'orange and [contributors][].

Maintenance fork (2026) by Savyasachi. Original: https://github.com/hyPiRion/lein-shell.
Distributed under the [Eclipse Public License 1.0](https://www.eclipse.org/legal/epl-v10.html). The original license remains in effect.

[contributors]: https://github.com/hyPiRion/lein-shell/contributors

Distributed under the Eclipse Public License, the same as Clojure.

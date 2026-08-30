# Documentation for lein-shell

lein-shell is a plugin that runs shell commands. Use it to run a setup tool that
the JVM does not provide or that has no Leiningen plugin.

Shell commands are usually straightforward. You can change the setup, working
directory, or exit code through `project.clj`.

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

## Basic Usage

Prepend a shell command with `lein shell` to run it. For example:

    lein shell cowsay -f duck 'Hello from Leiningen!'
     _______________________
    < Hello from Leiningen! >
     -----------------------
     \
      \
       \ >()_
          (__)__ _

lein-shell does not require a project, but projects are its usual use.

## Parameter Expansion

Like common Unix shells, lein-shell supports variable and parameter expansion in
arguments. When a string contains `${:foo}`, lein-shell expands it to `:foo` in
the project map, if that key exists. A vector such as `${[:foo :bar]}` is used
with `get-in` on the project map.

For example, this shell call prints information about the project. Use single quotes:

    lein shell cowsay -f bunny 'This is ${:name}, version ${:version}. It is licensed under the ${[:license :name]}'

For lein shell 1.0.0-SNAPSHOT, this prints the following in the terminal:

     ______________________________________
    / This is lein-shell, version          \
    | 1.0.0-SNAPSHOT. It is licensed under |
    \ the Eclipse Public License           /
     --------------------------------------
      \
       \   \
            \ /\
            ( )
          .( o ).

lein shell lets you specify a default string when values do not exist. Add `:-`
after the Clojure value, then add the default string and closing brace. For
example, this command prints the following in many projects:

    lein shell echo '${:not-defined:-this value is not defined!}'
    this value is not defined!

You can put parameter expansions in the default string. This example looks for
`:foo`, then `:bar`, then `:baz`, and then uses `quux`:

    lein shell echo 'Charging the lasers with ${:foo:-${:bar:-${:baz:-quux (:baz not available)} (:bar not available)} (:foo not available)}'
    Charging the lasers with quux (:baz not available) (:bar not available) (:foo not available)

Escape `$` to prevent parameter expansion:

    lein shell echo 'clojure.core\$apply.invoke is in the stacktrace'
    clojure.core$apply.invoke is in the stacktrace

When you specify this in project.clj, escape the escape character. The previous
call is equal to
`["shell" "echo" "clojure.core\\$apply.invoke is in the stacktrace"]`.

Parameter expansion does not support Clojure values that contain `}` or `:-`.
Use a plugin or a separate value for this complex expansion. Environment
variables cannot expand. This prevents accidental nonrepeatable builds. To use
environment variables, call a bash script with lein-shell.

## (Example) Usage

lein-shell is commonly used for preparation tasks and aliases.

### As a Preparation Task

Some projects must build files from scratch. For example, lein-shell can convert
`.less` files to `.css`. It can compile an ANTLR grammar before Clojure code.
This example generates code before uberjaring, testing, and REPL use:

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :prep-tasks [["shell" "generator" "--in" "build/in.grammar"
                                    "--out" "src/out.clj"]
               "javac" "compile"]
  :plugins [[net.clojars.savya/lein-shell "1.1.0"]])
```

`generator --in build/in.grammar --out src/out.clj` runs before every in-project evaluation.

lein-shell does not replace `make`. A shell command always runs. lein-shell can call `make`.

#### A Word About `:prep-tasks`

`:prep-tasks` are Leiningen tasks that run before a core task in a project.
By default, `:prep-tasks` is `["javac" "compile"]`. When you change it, the
default does not run. Add `javac` and `compile` before or after your changes
when you need them. In the example, the shell command can generate code for
`javac` or `compile`. Put the shell command last when you need compiled Java or Clojure code.

### As an Alias

lein-shell can also be used as an alias. This example uses the 1.7 javadoc for
Java documentation and the 1.6 JVM for backward compatibility. The shell call
uses an API that the 1.6 JVM cannot use:

```clj
(defproject package.name/project "0.1.0-SNAPSHOT"
  ...
  :plugins [[net.clojars.savya/lein-shell "1.1.0"]]
  :aliases {"javadoc" ["shell" "javadoc" "-d" "javadoc"
                        "-sourcepath" "src/" "package.name"]
            "jar" ["do" "javadoc," "jar"]}}})
```

Before deployment, this generates javadoc and includes it in the generated jar
file through inclusion filters. This prevents old javadoc files from inclusion.

If large builds take time, set up aliases for build tasks and run them once.
A shell file can do this, but aliases can keep the project free of shell scripts.

## Configuration

The default setup is sufficient for most uses. You can configure the environment
or the directory where the command runs.

### Settings and Their Priority

Options are specified inside the project as follows:

```clj
(defproject ...
  ...
  :shell {:option1 choice
          :option2 other-choice
          :commands {"foo" {:option1 choice-for-foo}}})
```

Set options as defaults for all shell commands or for one shell command. A
command-specific setting overrides the default setting. In the example,
`option2` is `other-choice` for all shell commands. `foo` has `option1` set to
`choice-for-foo`. Other commands have `option1` set to `choice`.

### Environment Variables

Set `:env` to a map of environment variables to configure them. The default is `{}`.

For example, change the default Java command, `JAVA_CMD`:

```clj
(defproject ...
  ...
  :shell {:env {"JAVA_CMD" "java42"})
```

Use `printenv` to check the change. `printenv JAVA_CMD` usually prints `java`
or nothing. `lein shell printenv JAVA_CMD` now prints `java42`.

### Directory Specification

Set `:dir` to a string to configure the directory where the command runs.
The default is the project root directory. You can set the directory relative to
the project root directory.

For example:

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :shell {:dir "src"})
```

Assume that the project is in `/home/pir/workspace/my-project`.
`lein shell pwd` then prints `/home/pir/workspace/my-project/src`.
If you change `"src"` to `"/home/pir"`, it prints `/home/pir`.

### Exit Codes

If a shell command returns a nonzero exit code, lein-shell tries to exit
Leiningen with the same exit code. Set `:exit-code` to `:ignore` to override
this behavior. lein-shell then ignores the exit code and continues as if it was 0.
The default option is `:default`.

For example:

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :shell {:commands {"false" {:exit-code :ignore}}})
```

`lein shell false` then returns 0. All other commands return their actual exit code.

### OS-Specific Subprocess Call

Different operating systems can use different commands for equivalent functions.
For example, use `foo` in Linux and `bar` in Windows. This setup runs the
command as a preparation task before compilation:

```clj
(defproject ...
  ...
  :prep-tasks [["shell" "foo" "arg1" "arg2"] "javac" "compile"]
  :shell {:commands {"foo" {:windows "bar"}}})
```

Here, `lein` runs `foo arg1 arg2` on non-Windows systems and `bar arg1 arg2` on
Windows. Both run before every task in this project.

This replacement option applies only to specific commands. You cannot set it as a general option.

Provide multiple commands in a vector when one OS needs several commands for an equivalent result:

```clj
(defproject ...
  ...
  :shell {:commands {"foo" {:windows ["bar" "baz" "bat"]}}})
```

The detectable OSes are `:freebsd`, `:linux`, `:macosx`, `:openbsd`, `:solaris`,
and `:windows`. New Leiningen releases can add OSes.

### Default-command and aliases

In addition to OS-specific subprocess commands, use `:default-command` as a
catch-all. This function lets you create aliases for one command.

Aliases let you specify different options for different calls. Use them to run
the same command in different default directories or with different OS-specific
subcalls.

Here, `echo` aliases to `true` to suppress `echo` output. `shout` aliases to
`echo`, independent of the original suppression.

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :shell {:commands {"echo" {:default-command "true"}
                     "shout" {:default-command "echo"
                              :env {"DEBUG" "t"}}}})
```

`lein shell echo foo` then does nothing. `lein shell shout foo` prints `foo`
with the `DEBUG` environment variable set.

`:default-command` overrides only lein-shell calls in this project. It does not apply aliases elsewhere.

### Ignoring stdin

The JVM differs from a shell language. When input is sent, lein-shell detects
the first command that can receive it and sends input to that command. For
example, if a Clojure `cat` function exists:

```bash
echo 'foo' | lein do shell echo 'bar', run
# can be
(echo 'foo' | echo 'bar') && cat
# which only prints out bar

# but it can also be
echo 'foo' | (echo 'bar' && cat)
# which prints out foo, followed by bar on a new line
```

This behavior has a race condition. Input can arrive before or after `shell echo 'bar'` runs.

`echo` does not use stdin data. Configure lein-shell not to send stdin data to
`echo` with this `project.clj` setup:

```clj
(defproject my-project "0.1.0-SNAPSHOT"
  ...
  :shell {:commands {"echo" {:use-stdin? false}}})
```

`lein shell echo ...` then does not try to read stdin in this project.

#### Multiple stdin consumers

lein-shell cannot detect whether a command needs stdin or how much input it
needs. When one Leiningen invocation runs multiple shell commands, data **may**
go to the wrong command. Multiple commands can usually read input from a
keyboard. Reading from a pipe can be unsafe. It is safe when the first command
ends before stdin data arrives for the second command.

Request a solution for this problem in an issue.

# lein-shell changelog

## [Unreleased]

## [1.1.0] - 2026-08-30

### Fixed

- Backslashes in command arguments are no longer eaten. A backslash was
  consumed before every character, which destroyed Windows paths
  (`C:\Users\bin`) and regex arguments (`\d+\s*`). A backslash is now an escape
  character only before `$` and before another backslash (and before `}` inside
  a `${x:-default}` value); anywhere else, including at the end of an argument,
  it stays literal. The documented escapes `\${...}` and `\\` are unchanged.

## [1.0.3] - 2026-08-17

### Fixed

- The documented `:use-stdin?` key now controls whether the spawned process
  inherits stdin; it previously did nothing (`*pump-in*` read the undocumented
  `:pipe-stdin?` instead). It defaults to true, with `:pipe-stdin?` honored as a
  backward-compatible alias.

## [1.0.2]

* Docs-only release: standardize the README to the canonical skeleton, cut
  self-promotional phrasing, and unify the status badges and CI workflow name.

## [1.0.1]

* Declared `leiningen-core` as a provided dependency so cljdoc can resolve the
  `leiningen.core.*` requires and build the API docs. Provided scope is
  non-transitive, so projects depending on lein-shell are unaffected.

## [1.0.0]

* Self-published as `net.clojars.savya/lein-shell` (maintenance fork).
* Migrated to `utils/get-os`, resolving the deprecation warning under Leiningen 2.12.

## [0.5.0]

* Fixed a bug where non-string were not converted to strings before sent to the
  command, causing NullPointerExceptions, cast errors and other problems.
* Implemented parameter expansion. You can specify them on the form `${:foo}`
  and on the form `${[:foo :bar]}`. They can also contain default strings.

## [0.4.2]

* Fixed a bug where lein-shell itself could not be used as a higher order alias
  in certain scenarios.

## [0.4.1]

* Custom `:shell :commands` can now be vectors instead of strings only, acting
  more or less like Lein aliases.

## [0.4.0]

* By default, stdin is piped in to shell commands. You can now specify that
  lein-shell doesn't pipe stdin to the shell command.
* A `:default-command` option can be specified as a catch-all for os-specific
  commands, which can be used to create aliases.

## [0.3.0]

* Exit-code handling, directory specification and environment settings can now
  be set on a per-command basis.
* It is now possible to ignore exit codes from commands, if wanted.
* Fixed a bug where reading from Stdin resulted in only partial input
  redirection.

## [0.2.0]

* Fixed a bug where the exit-code of a process was ignored.
* It is now possible to specify a directory in which to start commands. This is
  by default the root folder of a project.
* Added possibility to add/replace environment variable settings.
* Implemented functionality to specify aliases for commands based on which
  operative system you are using.

## [0.1.0]

* First release!

[0.5.0-tag]: https://github.com/hyPiRion/lein-shell/tree/0.5.0
[0.4.2-tag]: https://github.com/hyPiRion/lein-shell/tree/0.4.2
[0.4.1-tag]: https://github.com/hyPiRion/lein-shell/tree/0.4.1
[0.4.0-tag]: https://github.com/hyPiRion/lein-shell/tree/0.4.0
[0.3.0-tag]: https://github.com/hyPiRion/lein-shell/tree/0.3.0
[0.2.0-tag]: https://github.com/hyPiRion/lein-shell/tree/0.2.0
[0.1.0-tag]: https://github.com/hyPiRion/lein-shell/tree/0.1.0


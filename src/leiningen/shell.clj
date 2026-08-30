(ns leiningen.shell
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

(defn- failf [fmt-string & args]
  (throw (Exception. ^String (apply format fmt-string args))))

(declare read-expansion)

(defn- escape?
  "True when the backslash at position i escapes the character after it. A
  backslash is an escape character only before the characters in escapable;
  anywhere else it is a literal backslash."
  [^String s i escapable]
  (let [next-i (inc i)]
    (and (< next-i (.length s))
         (contains? escapable (.charAt s next-i)))))

(defn- read-default
  "read-default works like replace-values, but it stops when it finds a }.
  A backslash escapes $, \\ and }; before any other character it is literal.
  It returns [val end-pos], where val is the string read."
  [project ^String s orig-start start]
  (let [sb (StringBuilder.)]
    (loop [i start
           quoted false]
      (if (>= i (.length s))
        (failf "Unexpected end of argument '%s'. Opening ${ starts at %d, but is not closed"
               s (dec orig-start))
        (let [c (.charAt s i)]
          (if quoted
            (do (.append sb c)
                (recur (inc i) false))
            (case c
              \$ (let [[val end-pos] (read-expansion project s (inc i))]
                   (.append sb (str val))
                   (recur (inc end-pos) false))
              \} [(.toString sb) i]
              \\ (if (escape? s i #{\$ \\ \}})
                   (recur (inc i) true)
                   (do (.append sb c)
                       (recur (inc i) false)))
              (do (.append sb c)
                  (recur (inc i) false)))))))))

(defn- lookup-vector
  "Given a string and start and stop positions, returns its Clojure value.
  If the value is not a vector, it wraps the value in one."
  [^String s start end]
  (let [lookup-str (subs s start end)
        lookup-val (read-string lookup-str)]
    (if-not (vector? lookup-val)
      [lookup-val]
      lookup-val)))

(defn- read-expansion
  "read-expansion reads a parameter expansion. It does not quote values.
  Clojure values that contain :- or } cause an error in the expansion mechanism.
  See replace-values for information about parameter expansion."
  [project ^String s start]
  (if (not= (.charAt s start) \{)
    (failf "Expected { after $ at position %d in argument '%s', but was %c"
           start s (.charAt s start))
    (loop [i (inc start)
           colon false]
      (if (>= i (.length s))
        (failf "Unexpected end of argument '%s'. Opening ${ starts at %d, but is not closed"
               s (dec start))
        (let [c (.charAt s i)]
          (cond (and colon (= c \-))
                ;; ":-" ends the lookup value. A Clojure value with :- causes an error.
                (let [lookup-vec (lookup-vector s (inc start) (dec i))
                      [default end-pos] (read-default project s start (inc i))]
                  [(get-in project lookup-vec default) end-pos])
                (= c \}) ;; Values that contain } are not supported.
                (let [lookup-vec (lookup-vector s (inc start) i)]
                  [(get-in project lookup-vec) i])
                :else
                (recur (inc i) (= c \:))))))))

(defn- replace-values
  "replace-values takes a project and a string s. It applies parameter expansion
  to forms with the shape ${xxx}. xxx is a Clojure value or a vector of Clojure
  values. In ${xxx:-yyy}, yyy is the default value when xxx is absent from the
  project map. yyy expands recursively. The legal form ${xxx:-${xxx2:-yyy}}
  looks for xxx, then xxx2, then yyy.

  A backslash is an escape character only before $ and before another
  backslash. \"\\${:foo}\" becomes the string \"${:foo}\", and
  \"\\\\${:foo}\" becomes \"\\[expansion]\". Before any other character,
  and at the end of the string, a backslash stays as a literal backslash:
  \"C:\\\\Users\" and \"\\\\d+\" pass through unchanged. Inside a default
  value, a backslash also escapes the closing }.

  The form ${} is illegal."
  [project ^String s]
  (let [sb (StringBuilder. (.length s))]
    (loop [i (int 0)
           quoted false]
      (if (>= i (.length s))
        (.toString sb)
        (let [c (.charAt s i)]
          (if quoted
            (do (.append sb c)
                (recur (inc i) false))
            (case c
                \$ (let [[val end-pos] (read-expansion project s (inc i))]
                       (.append sb (str val))
                       (recur (int (inc end-pos)) false))
                \\ (if (escape? s i #{\$ \\})
                     (recur (inc i) true)
                     (do (.append sb c)
                         (recur (inc i) false)))
                (do (.append sb c)
                    (recur (inc i) false)))))))))

(defn- param-expand
  [project s]
  (if-not (string? s)
    (str s)
    (replace-values project s)))

(defmacro ^:private get-setting-fn
  "Returns a function that returns the highest-priority setting for a project
  and command. This is a macro because a function captures and dereferences
  dynamic variables. It returns falsey values."
  ([kw] `(get-setting-fn ~kw nil))
  ([kw default]
     `(let [gsym# (gensym "not-found")]
        (fn [project# [command# & args#]]
          (first
           (remove #(= gsym# %)
                   [(get-in project# [:shell :commands command# ~kw] gsym#)
                    (get-in project# [:shell ~kw] gsym#)
                    ~default]))))))

(def ^:private get-environment
  (get-setting-fn :env eval/*env*))

(def ^:private get-directory
  (get-setting-fn :dir eval/*dir*))

(def ^:private get-exit-code
  (get-setting-fn :exit-code :default))

(def ^:private get-pipe-stdin?
  (get-setting-fn :pipe-stdin? true))

;; `:use-stdin?` is the documented key; `:pipe-stdin?` is honored as a
;; backward-compatible alias. Default is true: lein-shell sends stdin to the
;; first command that can receive it unless a command opts out.
(def ^:private get-use-stdin?
  (let [use-stdin (get-setting-fn :use-stdin? ::unset)]
    (fn [project cmd]
      (let [v (use-stdin project cmd)]
        (if (= v ::unset) (get-pipe-stdin? project cmd) v)))))

(defn- lookup-command
  "Looks up the first part of a command and replaces it with an OS-specific
  version when one exists."
  [project cmd]
  (let [command (first cmd)
        os (utils/get-os)]
    (if-let [os-cmd (or (get-in project [:shell :commands command os])
                        (get-in project [:shell :commands command :default-command]))]
      (let [normalized-cmd (if (string? os-cmd) [os-cmd] os-cmd)]
        (main/debug (format "[shell] Replacing command %s with %s. (os is %s)"
                            command normalized-cmd os))
        (concat normalized-cmd (rest cmd)))
      cmd)))

(defn- shell-with-project [project cmd]
  (binding [eval/*dir* (get-directory project cmd)
            eval/*env* (get-environment project cmd)
            eval/*pump-in* (get-use-stdin? project cmd)]
    (let [cmd (lookup-command project cmd)]
      (main/debug "[shell] Calling the shell with" cmd)
      (apply eval/sh cmd))))

(defn ^:no-project-needed shell
  "Calls a shell command from Leiningen. Use it to add `make` or similar commands
to prep-tasks when no Leiningen plugin exists. If the process returns a nonzero
exit code, this command forces Leiningen to exit with the same exit code.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [project & cmd]
  (let [cmd (mapv #(param-expand project %) cmd)
        exit-code (shell-with-project project cmd)
        exit-code-action (get-exit-code project cmd)]
    (case exit-code-action
      :ignore (main/debug (format "[shell] Ignoring exit code (is %d)"
                                  exit-code))
      :default (if-not (zero? exit-code)
                 (main/exit exit-code)))))

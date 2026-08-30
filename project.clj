(defproject net.clojars.savya/lein-shell "1.1.0"
  :description "Call shell from within Leiningen."
  :url "https://github.com/jsavyasachi/lein-shell"
  :license {:name "Eclipse Public License 1.0"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true
  ;; provided (non-transitive) so cljdoc can resolve leiningen.core.* for API docs
  :profiles {:provided {:dependencies [[leiningen-core "2.13.0"]]}}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])

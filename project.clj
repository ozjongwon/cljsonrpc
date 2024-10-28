(defproject cljsonrpc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/data.json "2.5.0"]
                 [dev.weavejester/medley "1.8.1"]
                 ;;                 [clj-http "3.13.0"]
                 [org.clj-commons/gloss "0.3.6"]
                 [aleph "0.8.1"]]
  ;;  :profiles {:dev {:dependencies [[alembic "0.3.2"]]}}
  :repl-options {:init-ns cljsonrpc.core})

(comment
  (require '[alembic.still :refer [distill lein]])

  (defn add-project-dependency
    "Add project dependency at runtime via alembic."
    ([dep-vector]
     (let [[lib-name lib-version] dep-vector]
       (add-project-dependency lib-name lib-version)))
    ([lib-name lib-version]
     (let [dep-name (symbol lib-name)
           dep-version (name lib-version)]
       (alembic.still/distill [dep-name dep-version]))))
  )

(defproject alloy.stele/stele-server "0.0.1-SNAPSHOT"
            :plugins [[lein-modules "0.3.11"]]
            :description "FIXME: write description"
            :dependencies [
                           [alloy.stele/stele-core :version]
                           [ring "1.5.0"]
                           [compojure "1.5.1"]]
            :source-paths ["src"])
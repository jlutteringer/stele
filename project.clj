(defproject alloy.stele/stele "0.0.1-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "FIXME: write description"

  :profiles {
             :provided {
                        :dependencies [[org.clojure/clojure "_"] [org.clojure/clojurescript "_"]]
                        }}

  :modules {
            :inherited {:url     "http://example.com/FIXME"
                        :license {:name "Eclipse Public License"
                                  :url  "http://www.eclipse.org/legal/epl-v10.html"}}

            :versions  {org.clojure/clojure       "1.9.0-alpha16"
                        org.clojure/clojurescript "1.9.518"}})
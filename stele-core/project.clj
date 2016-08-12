(defproject alloy.stele/stele-core "0.0.1-SNAPSHOT"
	:plugins [[lein-modules "0.3.11"] [lein-cljsbuild "1.1.0"]]
	:description "FIXME: write description"
	:dependencies [[alloy/anvil-clojure "0.0.1-SNAPSHOT"]]
	:source-paths ["src/clj" "src/cljs" "src/cljc"]

	:cljsbuild {:builds [{:source-paths ["src/cljc"  "src/cljs"]

												:compiler {
																	 :output-to "target/assets/js/stele/core.js"
																	 :optimizations :whitespace
																	 :pretty-print true
																	 }}]}
	:clean-targets ^{:protect false} [:target-path "target/assets/js"])
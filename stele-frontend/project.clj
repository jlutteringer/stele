(defproject alloy.stele/stele-frontend "0.0.1-SNAPSHOT"
	:plugins [[lein-modules "0.3.11"] [lein-cljsbuild "1.1.0"]]
	:description "FIXME: write description"
	:dependencies [[alloy.stele/stele-core :version]]
	:source-paths ["src"]

	:cljsbuild {:builds [{:source-paths ["src"]
												:compiler {
																	 :output-to "target/assets/js/stele/core.js"
																	 :optimizations :whitespace
																	 :pretty-print true
																	 }}]}
	:clean-targets ^{:protect false} [:target-path "target/assets/js"])
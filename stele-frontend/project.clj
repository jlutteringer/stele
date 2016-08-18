(defproject alloy.stele/stele-frontend "0.0.1-SNAPSHOT"
	:plugins [[lein-modules "0.3.11"] [lein-cljsbuild "1.1.1"] [lein-npm "0.6.2"]]
	:description "FIXME: write description"
	:dependencies [[alloy.stele/stele-core :version]
								 [re-frame "0.8.0-alpha11"]
								 [re-com "0.8.3"]
								 [reagent "0.6.0-rc"]
								 [garden "1.3.2"]
								 [binaryage/devtools "0.8.1"]]
	:source-paths ["src"]

	:cljsbuild {:builds [{:source-paths ["src"]
												:compiler {
																	 :output-to "resources/stele.js"
																	 :optimizations :whitespace
																	 :pretty-print true
																	 }}]}
	:clean-targets ^{:protect false} [:target-path "target/assets/js"])
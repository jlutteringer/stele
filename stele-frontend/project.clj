(defproject alloy.stele/stele-frontend "0.0.1-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"] [lein-cljsbuild "1.1.1"] [lein-npm "0.6.2"]]
  :description "FIXME: write description"
  :dependencies [[alloy.stele/stele-core :version]
                 [re-frame "0.8.0-alpha11"]
                 [re-com "0.8.3"]
                 [reagent "0.6.0-rc"]
                 [garden "1.3.2"]
                 [binaryage/devtools "0.8.1"]
                 [com.taoensso/timbre "4.7.0"]
                 [com.rpl/specter "0.13.1-SNAPSHOT"]
                 [figwheel-sidecar "0.5.8"]]
  :source-paths ["src" "resources/scripts"]

  :cljsbuild {:builds [{:id           "dev"
                        :figwheel     true
                        :source-paths ["src"]
                        :compiler     {
                                       :main          "alloy.stele.frontend.main"
                                       :output-to     "resources/public/js/stele.js"
                                       :output-dir    "resources/public/js"
                                       :asset-path    "js"
                                       :optimizations :none
                                       :pretty-print  true
                                       :source-map    true
                                       }}]}
  :clean-targets ^{:protect false} [:target-path "resources/public/js"]
  :figwheel {
             :http-server-root "public"                     ;; this will be in resources/
             :server-port      3448                         ;; default is 3449
             :server-ip        "localhost"                  ;; default is "localhost"

             ;; CSS reloading (optional)
             ;; :css-dirs has no default value
             ;; if :css-dirs is set figwheel will detect css file changes and
             ;; send them to the browser
             :css-dirs         ["resources/public/css"]

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server
             ;:ring-handler example.server/handler

             ;; Clojure Macro reloading
             ;; disable clj file reloading
             ; :reload-clj-files false
             ;; or specify which suffixes will cause the reloading
             ; :reload-clj-files {:clj true :cljc false}

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path, a line number and a column
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2:$3 $1
             ;;
             ;:open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Load CIDER, refactor-nrepl and piggieback middleware
             ;;  :nrepl-middleware ["cider.nrepl/cider-middleware"
             ;;                     "refactor-nrepl.middleware/wrap-refactor"
             ;;                     "cemerick.piggieback/wrap-cljs-repl"]

             ;; if you need to watch files with polling instead of FS events
             ;; :hawk-options {:watcher :polling}
             ;; ^ this can be useful in Docker environments

             ;; if your project.clj contains conflicting builds,
             ;; you can choose to only load the builds specified
             ;; on the command line
             ;; :load-all-builds false ; default is true
             })
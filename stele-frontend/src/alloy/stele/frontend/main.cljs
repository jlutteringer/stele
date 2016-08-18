(ns alloy.stele.frontend.main
	(:require [reagent.core :as reagent]
						[alloy.stele.frontend.app :as app]
						[devtools.core :as devtools])
	(:require-macros [mount.core :refer [defstate]]))

(devtools/install!)
(app/start-app)
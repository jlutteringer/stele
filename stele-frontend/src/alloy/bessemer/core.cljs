(ns alloy.bessemer.core
	(:require
		[alloy.anvil.clojure.util :as util :include-macros true]
		[alloy.stele.frontend.reagent-util :as rutil :include-macros true]))

(defn container [& args]
	(util/concat-vec :div.container args))

(def col
	(rutil/build-component
		[[:size :description "This is the description" :default nil]
		 [:content :description "This is the content" :primary]]
		(fn [{:keys [size content]}]
			(util/concat-vec
				[:div {:class (str "col-xs" (when (some? size) (str "-" size)))}]
				content))))

(def button
	(rutil/build-component
		(fn [{:keys [type label]}]
			[:button {:class (str "btn btn-" (name type))} label])))
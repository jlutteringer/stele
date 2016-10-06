(ns alloy.bessemer.documentation.components
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.bessemer.util :as butil]
						[alloy.bessemer.documentation.core :as doc]
						[alloy.anvil.clojure.parse :as parse]))

(def example-schema (butil/web-element [::example
																				:fields [[:content]
																								 [:source]]]))
(def example
	(butil/component
		(fn [{:keys [content source]}]
			[:div.bessemer-example
			 (util/concat-vec  [:div.example] content)
			 (when (some? source)
				 [:div.documentation
					[:pre
					 (util/concat-vec :code.clojure (parse/clojure-to-hiccup source))]])])
		:schema example-schema
		:static))

(def sub-section
	(butil/component
		(fn [{:keys [key title description components]}]
			[:div.documentation-section
			 [:h1 title]
			 [:p description]
			 (util/concat-vec
				 :div.documentation-examples
				 (map (fn [{:keys [key title description example source additional-content]}]
								[:div.documentation-example
								 [:h2 title]
								 [:p description]
								 [alloy.bessemer.documentation.components/example
									:content example
									:source source]
								 additional-content])
							components))])
		:schema doc/sub-section-schema
		:static))

(def section ())

(def documentation ())
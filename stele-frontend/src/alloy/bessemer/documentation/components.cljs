(ns alloy.bessemer.documentation.components
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.bessemer.util :as butil]
						[alloy.bessemer.core :as b]
						[alloy.bessemer.documentation.core :as doc]
						[alloy.anvil.clojure.parse :as parse]
						[alloy.anvil.clojure.schema :as schema]))

(def example
	(butil/component
		(fn [{:keys [description example source]}]
			[:div.bessemer-example
			 [:p description]
			 [:div.example example]
			 (when (some? source)
				 [:div.documentation
					[:pre
					 (util/concat-vec :code.clojure (parse/clojure-to-hiccup source))]])])
		:schema doc/example-component-schema))

(def sub-section
	(butil/component
		(fn sub-section [{:keys [key title description components] :as args} ]
			[:div.documentation-section
			 [:h1 title]
			 [:p description]
			 [:div.documentation-examples
				(map (fn [{:keys [key title description examples additional-content]}]
							 [:div.documentation-example
								[:h2 title]
								(if (string? description) [:p description] description)
								[:div (map #(-> [alloy.bessemer.documentation.components/example %]) examples)]
								additional-content])
						 components)]])
		:schema doc/sub-section-schema))

(def section
	(butil/component
		(fn [{:keys [key title description sub-sections] :as args} ]
			[:div.bessemer
			 [:div.bessemer-section-header
				[b/container
				 [:h1 title]
				 [:p.lead description]]]
			 [b/container
				[:div.row
				 [b/col
					(util/map-vec #(-> [sub-section %]) sub-sections)
					:size 9]
				 [b/col "Sidebar"]]]])
		:schema doc/section-schema))

(def bessemer-documentation-site-schema
	(schema/substantiate-schema [::bessemer-documentation-site
															 :fields [[:sections]]]))

(def bessemer-documentation-site
	(butil/component
		(fn [{:keys [sections]}]
			[:div.bessemer (util/map-vec #(-> [section %]) sections)])
		:schema bessemer-documentation-site-schema))
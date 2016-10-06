(ns alloy.bessemer.core
	(:require
		[alloy.anvil.clojure.util :as util :include-macros true]
		[alloy.anvil.clojure.schema :as schema]
		[alloy.bessemer.documentation.core :as doc]
		[clojure.string :as string]
		[alloy.bessemer.util :as butil]))

(doc/def-section ::components "Components"
								 "Over a dozen reusable components built to provide buttons, dropdowns, input groups, navigation, alerts, and much more.")

(defn container [& args]
	(util/concat-vec :div.container args))

(def col-schema (butil/web-element [::col
																		:fields [[:size]
																						 [:content :primary]]]))
(def col
	(butil/component
		(fn [{:keys [size content]}]
			(util/concat-vec
				[:div
				 {:class (str "col-xs" (when (some? size) (str "-" size)))}] content))
		:schema col-schema
		:static))

(def anchor-schema (butil/web-element [::anchor
																			 :fields [[:label :primary]
																								[:uri]]]))
(def anchor
	(butil/component
		(fn [{:keys [label uri] :as args}]
			[:a (butil/make-attributes args {:class "" :href uri}) label])
		:schema anchor-schema
		:static))

(doc/def-sub-section ::button "Buttons"
								 "Use Bootstrap’s custom button styles for actions in forms, dialogs, and more. Includes support for a handful of contextual variations, sizes, states, and more.")

(def button-types #{:primary :secondary :success :info :warning :danger :link})
(def button-sizes #{:default :large :small :block})

(def button-schema (butil/web-element [::button
																			 :name "Buttons"
																			 :fields [[:label :primary]
																								[:type :default :primary :layout-type [:enumerated button-types]]
																								[:outline :default false :layout-type :flag]
																								[:size :default :default :layout-type [:enumerated button-sizes]]
																								[:disabled :default false :layout-type :flag]]]))
(def button
	(butil/component
		(fn [{:keys [label type outline size disabled] :as args}]
			(if (string? label)
				[:button (butil/make-attributes
									 args
									 {:class ["btn"
														(str "btn-" (when outline "outline-") (name type))
														(cond
															(= :default size) ""
															(= :large size) "btn-lg"
															(= :small size) "btm-sm"
															(= :block size) "btn-block")]}
									 (when disabled
										 {:disabled "disabled"})) label]
				(butil/make-merged-element label anchor-schema args {:class ["btn" (str "btn-" (name type))]})))
		:schema button-schema
		:static))

(doc/def-example ::examples "Examples"
								 "Bootstrap includes six predefined button styles, each serving its own semantic purpose."
								 [:div.btn-toolbar
									[button "Primary"]
									[button "Secondary" :secondary]
									[button "Success" :success]
									[button "Info" :info]
									[button "Warning" :warning]
									[button "Danger" :danger]
									[button "Link" :link]]
								 [:div.callout.callout-warning
									[:h4 "Conveying meaning to assistive technologies"]
									[:p "Using color to add meaning only provides a visual indication, which will not be conveyed to users of assistive technologies – such as screen readers. Ensure that information denoted by the color is either obvious from the content itself (e.g. the visible text), or is included through alternative means, such as additional text hidden with the .sr-only class."
									 ]])
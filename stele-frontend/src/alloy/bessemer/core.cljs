(ns alloy.bessemer.core
	(:require
		[alloy.anvil.clojure.util :as util :include-macros true]
		[alloy.anvil.clojure.schema :as schema]
		[alloy.anvil.clojure.parse :as parse]
		[clojure.string :as string]
		[alloy.bessemer.util :as butil]))

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
			[:a (butil/make-attributes label {:href uri} args)])
		:schema anchor-schema
		:static))

(def button-types #{:primary :secondary :success :info :warning :danger :link})
(def button-sizes #{:default :large :small :block})

(def example-schema (butil/web-element [::example
																				:fields [[:content]
																								 [:code]]]))
(def example
	(butil/component
		(fn [{:keys [content code]}]
			[:div.bessemer-example
			 (util/concat-vec  [:div.example] content)
			 (when (some? code)
				 [:div.documentation
					[:pre
					 (util/concat-vec :code.clojure (parse/clojure-to-hiccup code))]])])
		:schema example-schema
		:static))

(def button-schema (butil/web-element [::button
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
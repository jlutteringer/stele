(ns alloy.bessemer.core
	(:require
		[alloy.anvil.clojure.util :as util :include-macros true]
		[alloy.anvil.clojure.schema :as schema]
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
			[:a (butil/make-attributes {:href uri} args) label])
		:schema anchor-schema
		:static))

(def button-types #{:primary :secondary :success :info :warning :danger :link})

(def button-schema (butil/web-element [::button
																			 :fields [[:label :primary]
																								[:type :default :primary :layout-type [:enumerated button-types]]
																								[:outline :default false :layout-type :flag]
																								[:size :defauly]]]))

(def button
	(butil/component
		(fn [{:keys [label type] :as args}]
			(if (string? label)
				[:button (butil/make-attributes {:class ["btn" (str "btn-" (name type))]} args) label]
				(butil/make-merged-element label anchor-schema args {:class ["btn" (str "btn-" (name type))]})))
		:schema button-schema
		:static))
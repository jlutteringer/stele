(ns alloy.bessemer.util
	(:require [alloy.anvil.clojure.util :as util]
						[clojure.string :as string]
						[alloy.anvil.clojure.schema :as schema]
						[reagent.impl.template :as r-template]))

(defn as-form [element] (if (fn? element) (element) element))

(defn as-element [element]
	(r-template/as-element (as-form element)))

(def web-element-schema
	(schema/substantiate-schema [::web-element
															 :fields [[:class :default []]
																				[:style :default {}]]]))

(defn web-element [schema] (schema/substantiate-schema schema web-element-schema))

(defn attributes [web-element additional-attributes]
	(util/concat-map
		web-element
		additional-attributes
		{:class (string/join " " (util/concat-vec (:class web-element) (:class additional-attributes)))}))

(defn component [& args]
	(apply schema/component-handler args))

(defn web-element-field-map [web-element] )

(defn merge-elements [first second])
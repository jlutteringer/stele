(ns alloy.bessemer.util
	(:require [alloy.anvil.clojure.util :as util]
						[clojure.string :as string]
						[alloy.anvil.clojure.schema :as schema]
						[reagent.impl.template :as r-template]))

(defn handler-fn [f] (fn [& args] (apply f args) nil))
(defn as-form [element] (if (fn? element) (element) element))

(defn as-element [element]
	(r-template/as-element (as-form element)))

(def web-element-schema
	(schema/substantiate-schema [::web-element
															 :fields [[:class :default []]
																				[:style :default {}]]]))

(def web-element-merge-strategy
	{:class (fn [first second]
						(util/concat-vec (:class first) (:class second)))})

(defn web-element [schema] (schema/substantiate-schema schema web-element-schema))

(defn component [& args]
	(let [{:keys [template] :as substantiated-args} (schema/substantiate args schema/component-handler-schema)]
		(schema/component-handler
			(assoc substantiated-args :template
																(fn [& args]
																	(util/hiccupify (apply template args)))))))

(defn merge-elements [& args]
	(util/map-concat-strategy web-element-merge-strategy args))

(defn filter-web-attributes [web-element]
	(util/map-filter-keys (schema/get-field-keys web-element-schema) web-element))

(defn merge-attributes [web-attributes & target-attributes]
	(merge-elements
		(filter-web-attributes (merge-elements web-attributes))
		target-attributes))

(def web-element-substantiation-strategy
	{:class (fn [classes] (string/join " " classes))})

(defn substantiate-attributes [attributes]
	(util/map-transform-strategy web-element-substantiation-strategy attributes))

(defn make-attributes [web-attributes & target-attributes]
	(substantiate-attributes (merge-attributes web-attributes target-attributes)))

(defn make-merged-element [raw-element schema merge-element additions]
	[(first raw-element)
	 (merge-elements
		 (schema/substantiate (rest raw-element) schema)
		 (filter-web-attributes merge-element)
		 additions)])

(defn component-type? [component type] (= (first component) type))
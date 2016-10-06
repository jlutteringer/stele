(ns alloy.anvil.clojure.parse
	(:require [alloy.anvil.clojure.util :as util]
						[clojure.tools.reader.edn :as edn]
						[clojure.walk :as walk]))

(defn rollup [intermetiate-result]
	(walk/postwalk
		(fn [form]
			(if (sequential? form)
				(util/to-vec (util/flatten-1
											 (util/coll-vectorize
												 (fn [x] (not (and (sequential? x) (sequential? (first x))))) form)))
				form))
		intermetiate-result))

(defn transformer [x]
	(cond
		(keyword? x) (if (= :alloy.anvil.clojure.util/map x) x [:span {:class "keyword"} (str x)])
		(symbol? x) [:span {:class "symbol"} (str x)]
		(vector? x)
			(if (= (first x) :alloy.anvil.clojure.util/map)
				(util/concat-vec [[:span {:class "s-expr"} "{"]]
												 (rest x)
												 [[:span {:class "s-expr"} "}"]])
				(util/concat-vec [[:span {:class "s-expr"} "["]]
												 x
												 [[:span {:class "s-expr"} "]"]]))
		(list? x) (util/concat-vec [:div {:class "form"}
																[:span {:class "s-expr"} "("]]
																 x
																 [[:span {:class "s-expr"} ")"]])
		(string? x) [:span {:class "string"} (str x)]
		:else x))

(defn hiccup-transform [code] (rollup (walk/postwalk transformer (util/demapify code))))

(defn clojure-to-hiccup [code]
	(hiccup-transform (if (string? code) (edn/read-string code) code)))
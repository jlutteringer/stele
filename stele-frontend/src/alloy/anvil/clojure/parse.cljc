(ns alloy.anvil.clojure.parse
	(:require [alloy.anvil.clojure.util :as util]
						[clojure.tools.reader.edn :as edn]
						[clojure.walk :as walk]))

;todo
(defn rollup [intermetiate-result]
	(walk/postwalk
		(fn [form]
			(if (sequential? form)
				form
				form))
		intermetiate-result))

(defn transformer [x]
	(cond
		(keyword? x) [:span {:class "keyword"} (str x)]
		(symbol? x)  [:span {:class "symbol"} (str x)]
		(vector? x) (util/concat-vec [[:span {:class "s-expr"} "["]]
																 x
																 [[:span {:class "s-expr"} "]"]])
		(list? x) (util/concat-vec [:div {:class "form"}
																[:span {:class "s-expr"} "("]]
																 x
																 [[:span {:class "s-expr"} ")"]])
		:else x))

(defn hiccup-transform [code] (walk/postwalk transformer code))

(defn clojure-to-hiccup [code]
	(hiccup-transform (if (string? code) (edn/read-string code) code)))
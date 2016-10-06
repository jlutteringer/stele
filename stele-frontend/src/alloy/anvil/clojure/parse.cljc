(ns alloy.anvil.clojure.parse
	(:require [alloy.anvil.clojure.util :as util]
						[clojure.tools.reader.edn :as edn]
						[clojure.walk :as walk]))

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

(defn hiccup-transform [code] (util/hiccupify (walk/postwalk transformer (util/demapify code))))

(defn clojure-to-hiccup [code]
	(hiccup-transform (if (string? code) (edn/read-string code) code)))
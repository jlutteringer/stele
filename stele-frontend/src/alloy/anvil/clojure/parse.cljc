(ns alloy.anvil.clojure.parse
	(:require [instaparse.core :as insta]
						[alloy.anvil.clojure.util :as util]))

(defn rollup [parse-tree]
	(clojure.walk/postwalk
		(fn [form]
			(cond
				(and (vector? form)
						 (or (= (first form) :SYMBOL_HEAD)
								 (= (first form) :NAME)
								 (= (first form) :SYMBOL_REST)
								 (= (first form) :SYMBOL)
								 (= (first form) :STRING)
								 (= (first form) :KEYWORD)
								 (= (first form) :KWNAME)
								 (= (first form) :WS)))
				(clojure.string/join (rest form))
				:else form))
		parse-tree))

(defn parse [parser code]
	(rollup (insta/parse parser code)))

(def clojure-grammar
	(clojure.java.io/resource "clojure-grammar.bnf"))

(def clojure-parser (insta/parser clojure-grammar))

(defn parse-clojure [code] (parse clojure-parser code))

(defn span-generator
	[x]
	(fn [& args]
		[:span {:class x} (apply str args)]))

(defn special-symbol-span-generator
	[s]
	[:span {:class "symbol"} s])

(defn macro-span-generator
	[& args]
	(list [:span {:class "reader-char"} (first args)]
				(rest args)))

(defn reverse-macro-span-generator
	[& args]
	(list (first args)
				[:span {:class "reader-char"} (rest args)]))

(defn collection-span
	[& args]
	(util/concat-vec
		[[:span {:class "s-exp"} (first args)]]
		(util/flatten-1 (rest (util/flatten-1 (butlast args))))
		[[:span {:class "s-exp"} (last args)]]))

(defn hiccup-transform
	[d]
	(util/flatten-2
		(insta/transform
			{:simple_sym str
			 :simple_keyword str
			 :macro_keyword str
			 :ns_symbol str

			 :named_char str
			 :any_char str
			 :u_hex_quad str

			 ;; literals
			 :literal identity
			 :string (span-generator "string")
			 :regex (span-generator "regex")
			 :number (span-generator "number")
			 :character (span-generator "character")
			 :nil (span-generator "nil")
			 :boolean (span-generator "boolean")
			 :keyword (span-generator "keyword")
			 :symbol special-symbol-span-generator
			 :param_name (span-generator "keyword")

			 ;; reader macro characters
			 :reader_macro identity
			 :quote macro-span-generator
			 :backtick macro-span-generator
			 :unquote macro-span-generator
			 :unquote_splicing macro-span-generator
			 :tag macro-span-generator
			 :deref macro-span-generator
			 :gensym reverse-macro-span-generator
			 :lambda macro-span-generator
			 :meta_data macro-span-generator
			 :var_quote macro-span-generator
			 :host_expr macro-span-generator
			 :discard macro-span-generator
			 :dispatch macro-span-generator

			 ;; top level
			 :file (fn [& args] (concat args))
			 :forms (fn [& args] (identity args))
			 :form (fn [& args] (concat args))

			 ;; collections
			 :map collection-span
			 :list collection-span
			 :vector collection-span
			 :set collection-span

			 ;; extras
			 :comment (span-generator "comment")
			 :whitespace str}
			d)))

(defn clojure-to-hiccup [code]
	(hiccup-transform (parse-clojure code)))
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
	"file: form *;\n\nform: comment | whitespace | literal\n    | list\n    | vector\n    | map\n    | set\n    | reader_macro\n    ;\n\nforms: form* ;\n\nlist: '(' forms ')' ;\n\nvector: '[' forms ']' ;\n\nmap: '{' forms '}' ;\n\nset: '#{' forms '}' ;\n\nreader_macro\n    : lambda\n    | meta_data\n    | var_quote\n    | host_expr\n    | tag\n    | discard\n    | dispatch\n    | deref\n    | quote\n    | backtick\n    | unquote\n    | unquote_splicing\n    | gensym\n    ;\n\nquote\n    : '\\'' form\n    ;\n\nbacktick\n    : '`' form\n    ;\n\nunquote\n    : '~' form\n    ;\n\nunquote_splicing\n    : '~@' form\n    ;\n\ntag\n    : '^' form\n    ;\n\nderef\n    : '@' form\n    ;\n\ngensym\n    : SYMBOL '#'\n    ;\n\nlambda\n    : '#' list\n    ;\n\nmeta_data\n    : '#^' (map form | form)\n    ;\n\nvar_quote\n    : '#\\'' symbol\n    ;\n\nhost_expr\n    : '#+' form form\n    ;\n\ndiscard\n    : '#_' form\n    ;\n\ndispatch\n    : '#' symbol form\n    ;\n\nliteral\n    : string\n    | regex\n    | number\n    | character\n    | nil\n    | boolean\n    | keyword\n    | symbol\n    | param_name\n    ;\n\nstring: STRING;\n\nregex\n    : '#' STRING\n    ;\n\nnumber: NUMBER;\n\nNUMBER\n    : FLOAT\n    | HEX\n    | BIN\n    | BIGN\n    | LONG\n    ;\n\ncharacter\n    : named_char\n    | u_hex_quad\n    | any_char\n    ;\nnamed_char: CHAR_NAMED ;\nany_char: CHAR_ANY ;\nu_hex_quad: CHAR_U ;\n\nnil: NIL;\nboolean: BOOLEAN;\n\nkeyword: KEYWORD;\nKEYWORD: (':' | '::') KWNAME;\n\nsymbol: ns_symbol | simple_sym;\nsimple_sym: SYMBOL;\nns_symbol: NS_SYMBOL;\n\nparam_name: PARAM_NAME;\n\nSTRING : '\"' (#\"[^\\\"\\\\]\" | ESCAPE | CHAR_NAMED | CHAR_ANY | CHAR_U)* '\"' ;\n\nESCAPE:\n  '\\\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\\\\');\n\nFLOAT\n    : '-'? #\"[0-9]+\" FLOAT_TAIL\n    | '-'? 'Infinity'\n    | '-'? 'NaN'\n    ;\n\nFLOAT_TAIL\n    : FLOAT_DECIMAL FLOAT_EXP\n    | FLOAT_DECIMAL\n    | FLOAT_EXP\n    ;\n\nFLOAT_DECIMAL\n    : '.' #\"[0-9]+\"\n    ;\n\nFLOAT_EXP\n    : ('e' | 'E') '-'? #\"[0-9]+\"\n    ;\n\nHEX: '0' ('x' | 'X') HEXD+;\nHEXD: #\"[0-9a-fA-F]\";\nBIN: '0' ('b' | 'B') ('1' | '0')+ ;\nLONG: '-'? #\"[0-9]+[lL]?\";\nBIGN: '-'? #\"[0-9]+[nN]\";\n\nCHAR_U\n    : '\\\\' 'u' #\"[0-9D-Fd-f]\" HEXD HEXD HEXD ;\nCHAR_NAMED\n    : '\\\\' ( 'newline'\n           | 'return'\n           | 'space'\n           | 'tab'\n           | 'formfeed'\n           | 'backspace' ) ;\nCHAR_ANY\n    : '\\\\' #\".\" ;\n\nNIL : 'nil';\n\nBOOLEAN : 'true' | 'false' ;\n\nNS_SYMBOL\n    : NAME '/' SYMBOL\n    ;\n\nPARAM_NAME: '%' ((#\"[1-9]\" #\"[0-9]*\") | '&')? ;\n\nKWNAME: #\"([^^`'\\\"~@:/\\(\\)\\[\\]\\{\\} \\n\\r\\t\\,]+\\/)*[^^`'\\\"~@:/\\(\\)\\[\\]\\{\\} \\n\\r\\t\\,]+\";\n\nSYMBOL\n    : '.'\n    | '/'\n    | NAME\n    ;\n\nNAME: SYMBOL_HEAD SYMBOL_REST* (':' SYMBOL_REST+)* ;\n\nSYMBOL_HEAD: #\"[^0-9^`'\\\"#~@:/%\\(\\)\\[\\]\\{\\} \\n\\r\\t\\,]\";\n\nSYMBOL_REST\n    : SYMBOL_HEAD\n    | #\"[0-9]\"\n    | '.'\n    ;\n\nwhitespace: WS;\nWS: #\"[ \\n\\r\\t,]+\";\n\ncomment: COMMENT_CHAR;\nCOMMENT_CHAR: ';' #\"[^\\n\\r]+\" '\\n';")

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
		(rest (butlast args))
		[[:span {:class "s-exp"} (last args)]]))

(defn hiccup-transform
	[parse-tree]
	(let [intermetiate-result
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
					parse-tree)]
		(util/to-vec (util/flatten-with-pred
									 (fn [form]
										 (or (and (sequential? form) (sequential? (first form)))
												 (and (sequential? form) (string? (first form)))))
									 intermetiate-result))))

(defn clojure-to-hiccup [code]
	(hiccup-transform (parse-clojure code)))
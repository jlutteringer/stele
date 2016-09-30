(ns alloy.anvil.clojure.util
	#?(:cljs (:require-macros alloy.anvil.clojure.util))
	(:require [taoensso.timbre :as timbre #?@(:cljs (:include-macros true))]
						[com.rpl.specter :as specter #?@(:cljs (:include-macros true))]
		#?(:clj [clojure.spec :as spec]
			 :cljs [cljs.spec :as spec])))

; =====================================================================
; =====================================================================
; CORE UTILITIES
; =====================================================================
; =====================================================================

(defn pour
	"Wraps argument in a vector if it is not already a collection."
	[arg]
	(if (coll? arg)
		arg
		[arg]))

(defn debug [arg]
	(print arg)
	arg)

(defn concat-seq [& args]
	(apply concat (map pour args)))

(defn concat-vec [& args]
	(into [] (apply concat-seq args)))

(def not-zero? (complement zero?))
(def not-nil? (complement nil?))
(def not-neg? (complement neg?))
(defn not-empty? [col] (boolean (seq col)))

(def concrete-seq (filter false []))

(defn group-by-index [ccol]
	(loop [remainder ccol
				 result []]
		(if (every? empty? remainder)
			result
			(recur
				(map rest remainder)
				(conj result (map first remainder))))))

(defn coll-contains? [col v] (boolean (some #(= v %) col)))

(defn to-vec [col] (into [] col))
(defn to-set [col] (into #{} col))

(defn fn-rest [delegate]
	(fn [& args] (apply delegate (rest args))))

(defn find-first
	[f coll]
	(first (filter f coll)))

(defn take-even [x]
	(take-nth 2 x))

(defn take-odd [x]
	(take-nth 2 (drop 1 x)))

(defn route-fn [& functions]
	(fn [& args]
		(doseq [function functions]
			(apply function args))))

(def rand-uuid #?(:clj  #(java.util.UUID/randomUUID)
						 			:cljs cljs.core/random-uuid))

;TODO necessary?
(def true-identical? #?(:clj identical?
											 :cljs cljs.core/keyword-identical?))

(def spec-invalid #?(:clj  :clojure.spec/invalid
										 :cljs :cljs.spec/invalid))

(defn spec-invalid-result? [result] (true-identical? result spec-invalid))

(defn gen-symbol
	([]
	 (gen-symbol "_"))
	([prefix]
	 (-> prefix (str (rand-uuid)) symbol)))

(defn countf [f c]
	(->> c (filter f) count))

(def curry-blank? #(= % '_))

(defn- curry-gather-args [args]
	(let [symbols (map #(if (curry-blank? %) (gen-symbol) nil) args)]
		[(filter (comp not nil?)  symbols)
		 (map (partial reduce (fn [arg symbol] (if (curry-blank? arg) symbol arg)))
					(map vector args symbols))]))

(defmacro curry [func & args]
	(let [gathered-args (curry-gather-args args)]
		(list 'fn (into [] (first gathered-args))
					(conj (second gathered-args) func))))

(defn assert-error [& messages]
	#?(:clj (throw (new AssertionError (apply pr-str messages)))
		 :cljs (throw (js/Error. (apply pr-str messages)))))

(defn position-merge-helper [values target result]
	(cond (empty? values) (concat result target)
				(empty? target) (concat result (map first values))
				:else
				(if (zero? (second (first values)))
					(recur (map #(vector (first %) (dec (second %))) (rest values))
								 target
								 (conj result (first (first values))))
					(recur (map #(vector (first %) (dec (second %))) values)
								 (rest target)
								 (conj result (first target))))))

;[["a" 0] ["c" 2]]
;["b" "d"]
;["a" "b" "c" "d"]
(defn position-merge [values target]
	(position-merge-helper (sort-by second values) target []))

(defn rand-between [left-bound right-bound]
	(+ (rand (- right-bound left-bound)) left-bound))

(defn pairs-to-map [pairs] (into {} pairs))

(defn seq-to-map [list]
	(into {} (map vec (partition 2 list))))

(defn flatten-1
	"Flattens only the first level of a given sequence, e.g. [[1 2][3]] becomes
	 [1 2 3], but [[1 [2]] [3]] becomes [1 [2] 3]."
	[seq]
	(if (or (not (seqable? seq)) (nil? seq))
		seq ; if seq is nil or not a sequence, don't do anything
		(loop [acc [] [elt & others] seq]
			(if (nil? elt) acc
										 (recur
											 (if (seqable? elt)
												 (apply conj acc elt) ; if elt is a sequence, add each element of elt
												 (conj acc elt))      ; if elt is not a sequence, add elt itself
											 others)))))

(defn map-to-seq [map] (flatten-1 (seq map)))
(defn map-to-vec [map] (into [] (map-to-seq map)))

(defn filter-groups [groups filter-fn]
	(pairs-to-map (map #(-> [% (filter-fn %)]) groups)))

(defn third
	[coll]
	(first (next (next coll))))

(defn conform-or-throw [spec arg]
	(let [result (spec/conform spec arg)]
		(if (spec-invalid-result? result)
			(assert-error (spec/explain-str spec arg))
			result)))

(defn fn-metadata [func]
	{:name (first func)
	 :args (second func)
	 :body (drop 2 func)})

(defn static-fn [result] (fn [& _] result))

; =====================================================================
; =====================================================================
; =====================================================================
; =====================================================================

;(defn args-to-map [args]
;	(cond
;		(seq? args)
;		(if (= (count args) 1)
;			(first args)
;			(seq-to-map args))
;		(map? args) args))
;
;
;(defn schema-reified? [schema]
;	(every? map? (:fields schema)))
;
;(defn reify-schema-field [schema-field]
;	(merge {:name (first schema-field)} (seq-to-map (rest schema-field))))
;
;(defn make-default-pair [field default-overrides]
;	(let [field-name (:name field)]
;		(if (contains? default-overrides field-name)
;			[field-name (get default-overrides field-name)]
;			(cond
;				(contains? field :default) [field-name (:default field)]
;				(contains? field :default-fn) [field-name ((:default-fn field))]
;				:else nil))))
;
;(defn remove-nil [col] (remove nil? col))
;
;(defn defaults-map [default-overrides schema]
;	(pairs-to-map (remove-nil (map
;														 #(make-default-pair % default-overrides)
;														 (:fields schema)))))
;
;(defn build-args
;	([args schema] (build-args args {} schema))
;	([args default-overrides schema]
;	 (merge (defaults-map default-overrides schema) args)))
;
;(defn build-default-overrides [current-args previous-args previous-conformed-args]
;	(pairs-to-map (remove-nil (map
;															#(let [current-val (get current-args %)
;																		 prev-val (get previous-args %)]
;																(if (= current-val prev-val)
;																	[% (get previous-conformed-args %)]
;																	nil))
;															(keys previous-conformed-args)))))
;
;(defn conform-args
;	([args schema]
;	 (conform-args args {} schema))
;	([args default-overrides schema]
;	 (let [reified-schema (reify-schema schema)]
;		 (if (nil? (:spec reified-schema))
;			 (build-args args default-overrides reified-schema)
;			 (conform-or-throw (:spec reified-schema)
;												 (build-args args default-overrides reified-schema))))))
;
;(defn schema-fields [schema] ())

;(defn build-let-vec [arg-symbol conformed-arg-symbol schema template-args]
;	(concat-vec
;		[conformed-arg-symbol `(conform-args (args-to-map ~arg-symbol) ~schema)]
;		(if (nil? (first template-args))
;			[]
;			[(first template-args) conformed-arg-symbol])))

;(defn build-fn-body-internal [arg-symbol conformed-arg-symbol schema template]
;	(let [template-metadata (fn-metadata template)]
;		`(let ~(build-let-vec arg-symbol conformed-arg-symbol schema (:args template-metadata))
;			 (timbre/debug
;				 "In template fn with schema:" ~schema "Resolved args" ~conformed-arg-symbol "from actual args" ~arg-symbol)
;			 ~@(:body template-metadata))))
;
;(defmacro build-fn-body
;	([arg-symbol schema template]
;	 (build-fn-body-internal arg-symbol (gen-symbol) schema template))
;	([arg-symbol conformed-arg-symbol schema template]
;	 (build-fn-body-internal arg-symbol conformed-arg-symbol schema template)))
;
;(defn build-handler-fn-body-internal
;	([arg-symbol schema handler-template]
;		(build-handler-fn-body-internal arg-symbol schema '(fn [] {}) handler-template))
;	([arg-symbol schema initializer-template handler-template]
;	 (let [reified-schema (reify-schema schema)
;				 initializer-metadata (fn-metadata initializer-template)
;				 handler-metadata (fn-metadata handler-template)
;				 converted-arg-symbol (gen-symbol)
;				 outer-confomed-args-symbol (gen-symbol)
;				 initialization-result-symbol (gen-symbol)
;				 previous-args-atom-symbol (gen-symbol)
;				 inner-arg-symbol (gen-symbol)
;				 converted-inner-arg-symbol (gen-symbol)
;				 inner-confomed-args-symbol (gen-symbol)
;				 previous-args-symbol (gen-symbol)]
;		 (build-fn-body-internal arg-symbol outer-confomed-args-symbol reified-schema
;												`(fn ~(:args initializer-metadata)
;													 (let [~initialization-result-symbol (do ~@(:body initializer-metadata))
;																 ~converted-arg-symbol (args-to-map ~arg-symbol)
;																 ~previous-args-atom-symbol (atom [~converted-arg-symbol ~outer-confomed-args-symbol])]
;														 (fn [& ~inner-arg-symbol]
;															 (let [~converted-inner-arg-symbol (args-to-map ~inner-arg-symbol)
;																		 ~previous-args-symbol (deref ~previous-args-atom-symbol)
;																		 ~inner-confomed-args-symbol (conform-args ~converted-inner-arg-symbol
;																																							 (build-default-overrides
;																																								 ~converted-inner-arg-symbol
;																																								 (first ~previous-args-symbol)
;																																								 (second ~previous-args-symbol)) ~reified-schema)
;																		 ~(first (:args handler-metadata)) (merge ~inner-confomed-args-symbol
;																																							~initialization-result-symbol)]
;																 (reset! ~previous-args-atom-symbol [~converted-inner-arg-symbol ~inner-confomed-args-symbol])
;																 ~@(:body handler-metadata)))))))))
;
;(defn build-handler-fn-internal
;	([handler-template]
;	 (build-handler-fn-internal {} '(fn [] {}) handler-template))
;	([schema handler-template]
;	 (build-handler-fn-internal schema '(fn [] {}) handler-template))
;	([schema initializer-template handler-template]
;	 (let [args-symbol (gen-symbol)]
;		 `(fn [& ~args-symbol]
;				~(build-handler-fn-body-internal args-symbol schema initializer-template handler-template)))))
;
;(defmacro build-handler-fn-body [arg-symbol schema initializer-template handler-template]
;	(build-handler-fn-body-internal arg-symbol schema initializer-template handler-template))
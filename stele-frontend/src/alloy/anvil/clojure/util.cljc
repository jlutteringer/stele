(ns alloy.anvil.clojure.util
	#?(:cljs (:require-macros alloy.anvil.clojure.util))
	(:require [clojure.walk :as walk]
						[taoensso.timbre :as timbre #?@(:cljs (:include-macros true))]
						[com.rpl.specter :as specter #?@(:cljs (:include-macros true))]
		#?(:clj [clojure.spec :as spec]
			 :cljs [cljs.spec :as spec])))

; =====================================================================
; =====================================================================
; CORE UTILITIES
; =====================================================================
; =====================================================================

(def concrete-seq (filter false []))

(defn filter-null [coll] (filter some? coll))

(defn pour
	"Wraps argument in a vector if it is not already a collection."
	[arg]
	(if (sequential? arg)
		arg
		[arg]))

(defn debug [arg]
	(println "DEBUG" arg)
	arg)

(defn pair? [val] (and (sequential? val) (= 2 (count val))))

(defn to-vec [col] (into [] col))
(defn to-set [col] (into #{} col))

(defn concat-seq [& args]
	(apply concat (map pour (filter-null args))))

(defn concat-vec [& args]
	(into [] (apply concat-seq args)))

(defn coll-flatten-top-with-pred [pred arg]
	(filter (complement pred) (tree-seq pred seq arg)))

(defn coll-flatten-top-seqs [coll] (coll-flatten-top-with-pred sequential? coll))

(defn coll-vectorize [pred coll]
	(map (fn [x] (if (pred x) [x] x)) coll))

(defn map-merge-with-key
	"Returns a map that consists of the rest of the maps conj-ed onto
	the first.  If a key occurs in more than one map, the mapping(s)
	from the latter (left-to-right) will be combined with the mapping in
	the result by calling (f val-in-result val-in-latter)."
	[f & maps]
	(when (some identity maps)
		(let [merge-entry (fn [m e]
												(let [k (first e) v (second e)]
													(if (contains? m k)
														(assoc m k (f m (get m k) v))
														(assoc m k v))))
					merge2 (fn [m1 m2]
									 (reduce merge-entry (or m1 {}) (seq m2)))]
			(reduce merge2 maps))))

(defn map-convert-pairs [pairs] (into {} pairs))

(defn map-concat-with [f & args]
	(apply (partial map-merge-with-key f) (coll-flatten-top-with-pred sequential? args)))

(defn map-concat-strategy [strategy & args]
	(map-concat-with (fn [key first second]
										 (if (contains? strategy key)
											 ((get strategy key) first second)
											 second)) args))

(defn map-concat [& args] (map-concat-with (fn [_ _ val] val) args))

(defn map-filter [pred map] (map-convert-pairs (filter pred map)))

(defn map-filter-keys [keys map]
	(map-filter (fn [[key _]] (contains? keys key)) map))

(defn map-transform [f m] (map-convert-pairs (map f m)))

(defn map-transform-strategy [strategy map]
	(map-transform (fn [[key val]]
									 (if (contains? strategy key)
										 [key ((get strategy key) val)]
										 [key val])) map))

(defn map-contextual [f coll]
	(reverse (reduce
						 (fn [context val]
							 (conj context (f val context)))
						 concrete-seq
						 coll)))

(defn map-vec [f coll] (to-vec (map f coll)))

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

(defn flatten-2 [seq] (flatten-1 (flatten-1 seq)))

(defn set-build [& args]
	(to-set (filter some? args)))

(defn map-to-seq [map] (flatten-1 (seq map)))
(defn map-to-vec [map] (into [] (map-to-seq map)))

(defn demapify [x]
	(walk/postwalk (fn [form] (if (map? form) (concat-vec ::map (map-to-vec form)) form)) x))

(def not-zero? (complement zero?))
(def not-nil? (complement nil?))
(def not-neg? (complement neg?))
(defn not-empty? [col] (boolean (seq col)))

(defn group-by-index [ccol]
	(loop [remainder ccol
				 result []]
		(if (every? empty? remainder)
			result
			(recur
				(map rest remainder)
				(conj result (map first remainder))))))

(defn coll-contains? [col v] (boolean (some #(= v %) col)))

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

(defn seq-to-map [list]
	(into {} (map vec (partition 2 list))))

(defn filter-groups [groups filter-fn]
	(map-convert-pairs (map #(-> [% (filter-fn %)]) groups)))

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

(defn valueate [x] (if (fn? x) (x) x))
(defn ignore-args [x] (fn [& _] (valueate x)))
(defn static-fn [result] (ignore-args result))
(def empty-fn (static-fn nil))

(defn remove-nil [col] (remove nil? col))

(defn de-nest [arg]
	(walk/postwalk
		(fn [form]
			(if (sequential? form)
				(to-vec (flatten-1
									(coll-vectorize
										(fn [x] (not (and (sequential? x) (sequential? (first x))))) form)))
				form))
		arg))

(defn hiccupify [intermetiate-result]
	(walk/postwalk
		(fn [form]
			(if (and (sequential? form) (not (fn? (first form))))
				(to-vec (flatten-1
									(coll-vectorize
										(fn [x] (not (and (sequential? x) (or (sequential? (first x)) (empty? x))))) form)))
				form))
		intermetiate-result))
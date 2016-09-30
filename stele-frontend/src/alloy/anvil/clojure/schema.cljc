(ns alloy.anvil.clojure.schema
	(:require [alloy.anvil.clojure.util :as util]
						[taoensso.timbre :as timbre #?@(:cljs (:include-macros true))]
						[com.rpl.specter :as specter #?@(:cljs (:include-macros true))]))

(def schema-layout-types #{:default :primary :enumerated :flag})

(def schema-field-schema
	{:name :schema-field
	 :description "This is a description"
	 :fields [{:key :key :description "The unique (per schema) key of the schema field" :layout-type :primary}
						{:key :description :description "The description of the schema field" :layout-type :default}
						{:key :layout-type :description "The layout of the schema field for args coercion" :layout-type [:enumerated schema-layout-types] :default :default}
						{:key :default :description "The default value for args coercion" :layout-type :default}
						{:key :default-fn :description "The default fn to be ran at the time of args coercion" :layout-type :default}
						{:key :schema :description "The schema type of the field" :layout-type :default}]})

(def schema-schema
	{:name :schema
	 :description "This is a description"
	 :fields [{:key :name :description "The name of the schema" :layout-type :primary}
						{:key :description :description "The description of the schema" :layout-type :default}
						{:key :fields :description "The fields of the schema" :layout-type :default :schema [:collection schema-field-schema]}]})

(defn get-layout-details [field]
	(let [layout-type (:layout-type field)]
		(if (coll? layout-type)
			{:type (first layout-type) :args (rest layout-type)}
			{:type layout-type :args []})))

(defn filter-fields [pred schema] (filter pred (:fields schema)))
(defn get-field [key schema] (first (filter-fields #(= key (:key %)) schema)))
(defn filter-fields-by-layout [layout schema]
	(filter-fields #(= layout (:type (get-layout-details %))) schema))

(defn group-fields-by-layouts [schema]
	(util/filter-groups schema-layout-types #(filter-fields-by-layout % schema)))

;TODO refactor
(defn build-arg-pairs [args schema]
	(let [layout-to-field-key-map
				(specter/multi-transform (specter/multi-path
																	 [specter/MAP-VALS specter/ALL (specter/terminal #(-> [(:key %) (:args (get-layout-details %))]))]
																	 [specter/MAP-VALS (specter/terminal pairs-to-map)])
																 (group-fields-by-layouts schema))]
		(reduce (fn [context val]
							(let [previous-result (first context)]
								(cond
									(= ::key (first previous-result))
									(conj (rest context) [(second previous-result) val])
									(contains? (:default layout-to-field-key-map) val)
									(conj context [::key val])
									(contains? (:flag layout-to-field-key-map) val)
									(conj context [:flag val])
									:else (let [potential-enumerated-val
															(util/find-first (fn [entry] (contains? (first (second entry)) val))
																					(specter/select [:enumerated specter/ALL] layout-to-field-key-map))]
													(if (some? potential-enumerated-val)
														(conj context [(first potential-enumerated-val) val])
														(conj context [(first (keys (:primary layout-to-field-key-map))) val])))))) util/concrete-seq args)))

(defn schema-args-to-map [args schema] (util/pairs-to-map (build-arg-pairs args schema)))

(defn build-defaults-map-from-schema-fields [fields]
	(util/pairs-to-map (map #(cond
											 (some? (:default %)) [(:key %) (:default %)]
											 (some? (:default-fn %)) [(:key %) ((:default-fn %) fields)]
											 :else nil) fields)))

(defn normalize-schema-tag [schema-tag]
	(cond
		(nil? schema-tag) nil
		(vector? schema-tag) schema-tag
		:else [:default schema-tag]))

(defn apply-defaults-shallow [arg-map schema]
	(merge arg-map
				 (build-defaults-map-from-schema-fields
					 (filter-fields #(not (contains? arg-map (:key %))) schema))))

(defn apply-defaults [arg-map schema]
	(let [default-map-pairs (seq (apply-defaults-shallow arg-map schema))]
		(util/pairs-to-map (map (fn [[key value]]
												 (let [sub-schema-tag (normalize-schema-tag (:schema (get-field key schema)))]
													 (cond
														 (empty? sub-schema-tag)
														 [key value]
														 (= :collection (first sub-schema-tag))
														 [key (util/to-vec (map #(reify % (second sub-schema-tag)) value))]
														 :else
														 [key (reify value (second sub-schema-tag))])))
											 default-map-pairs))))

(defn mapify-shallow [args schema]
	(if (map? args) args (schema-args-to-map args schema)))

;TODO don't implement using recursion
(defn mapify [args schema]
	(let [reified-map-pairs (seq (mapify-shallow args schema))]
		(util/pairs-to-map (map (fn [[key value]]
												 (let [sub-schema-tag (normalize-schema-tag (:schema (get-field key schema)))]
													 (cond
														 (empty? sub-schema-tag)
														 [key value]
														 (= :collection (first sub-schema-tag))
														 [key (util/to-vec (map #(reify % (second sub-schema-tag)) value))]
														 :else
														 [key (reify value (second sub-schema-tag))])))
											 reified-map-pairs))))

(defn mapify-args [args schema]
	(if (and (= (count args) 1)
					 (or (map? (first args)) (vector? (first args))))
		(mapify (first args) schema)
		(mapify args schema)))

(defn reify [args schema] (apply-defaults (mapify args schema) schema))

(defn reify-schema [schema]
	(reify schema schema-schema))

;(defn conform [args schema]
;	(let
;		[prevalidated-result (reify args schema)]
;		(if (some? (:spec schema))
;			(conform-or-throw (:spec schema) prevalidated-result)
;			prevalidated-result)))

(defn make-fn
	([schema f]
	 (make-fn schema f (util/static-fn {})))

	([schema f default-overrider]
	 (fn [& args]
		 (let [mapified-args (mapify args schema)
					 reified-args (apply-defaults
													(merge mapified-args (default-overrider mapified-args))
													schema)]
			 (timbre/debug
				 "schema/make-fn with schema:" schema "Reified args" reified-args "from actual args" args)
			 (f mapified-args reified-args)))))

(defn make-handler-fn [schema initializer f]
	(let []
		(make-fn schema
						 (fn [initial-args]
							 (let [initializer-results (initializer initial-args)]
								 (make-fn schema
													(fn [actual-args]
														(f (merge actual-args initializer-results)))))))))

(def dropdown-schema
	(reify-schema ["Dropdown Element"
								 :fields [[:label :description "This is the description"]
													[:content :description "This is the content"]
													[:open-state :description "Open state atom" :default-fn #(atom false)]]]))

(def my-handler (make-handler-fn schema-schema (fn [_] {:a "a"}) (fn [arg] arg)))

{:name :size
 :description "The number of columns spanned by this column element in the 12 column grid"
 :default nil
 :layout-type :normal}

[:size :description "The number of columns spanned by this column element in the 12 column grid"]

{:name :content
 :description "The content of this column element"
 :default []
 :layout-type :primary}

[:content :description "The content of this column element" :primary]

["Column Element"
 :fields [[:size :description "The number of columns spanned by this column element in the 12 column grid"]
					[:content :description "The content of this column element" :primary]]]
(ns alloy.anvil.clojure.schema
	(:require [alloy.anvil.clojure.util :as util]
						[taoensso.timbre :as timbre #?@(:cljs (:include-macros true))]
						[com.rpl.specter :as specter #?@(:cljs (:include-macros true))]))

(def schema-layout-types #{:default :primary :enumerated :flag})

(defn get-layout-details [field]
	(let [layout-type (:layout-type field)]
		(if (coll? layout-type)
			{:type (first layout-type) :args (rest layout-type)}
			{:type layout-type :args []})))

(defn filter-fields [pred schema] (filter pred (:fields schema)))
(defn get-field [key schema] (first (filter-fields #(= key (:key %)) schema)))
(defn get-field-keys [schema] (util/to-set (map :key (:fields schema))))
(defn filter-fields-by-layout [layout schema]
	(filter-fields #(= layout (:type (get-layout-details %))) schema))

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
	{:key ::schema
	 :name "Schema"
	 :description "This is a description"
	 :fields [{:key :key :description "The key of the schema" :layout-type :primary}
						{:key :name :description "The name of the schema" :layout-type :default :default-fn #(name (:key %))}
						{:key :description :description "The description of the schema" :layout-type :default}
						{:key :fields :description "The fields of the schema" :layout-type :default :schema [:collection schema-field-schema]}]})

(defn group-fields-by-layouts [schema]
	(util/filter-groups schema-layout-types #(filter-fields-by-layout % schema)))

; TODO 'tagging' is a wip for more intelligent schema resolution, replacing build-arg-pairs
; this should reduce the need to disambiguate in some cases, and we should be able to provide
; better error handling when the resolution mechanism fails
(defn tag-args [args schema]
	(let [field-keys (get-field-keys schema)
				layout-to-field-key-map
				(specter/multi-transform (specter/multi-path
																	 [specter/MAP-VALS specter/ALL (specter/terminal #(-> [(:key %) (:args (get-layout-details %))]))]
																	 [specter/MAP-VALS (specter/terminal util/map-convert-pairs)])
																 (group-fields-by-layouts schema))]
		(util/map-contextual
			(fn [arg context]
				(let [potential-enumerated-val
							(util/find-first (fn [entry] (contains? (first (second entry)) arg))
															 (specter/select [:enumerated specter/ALL] layout-to-field-key-map))]
					[arg (util/set-build
								  (when (or (contains? (second (first context)) [:strong-key])
														(contains? (second (first context)) [:weak-key])) [:value])
								  (when (util/not-empty? (:primary layout-to-field-key-map)) [:primary])
									(cond
										(contains? (:default layout-to-field-key-map) arg) [:strong-key]
										(contains? field-keys arg) [:weak-key])
									(when (contains? (:flag layout-to-field-key-map) arg) [:flag])
									(when (some? potential-enumerated-val) [:enumerated (first potential-enumerated-val)]))]))
			args)))

(defn contains-tags [tags & target-tags]
	())

(defn choose-field-type [tagged-fields field type]
	())

(defn sp-next [arg])
(defn sp-skip [arg])

;TODO utility for advanced sequence processing
(defn seq-process [[field tags] result context]
	(if (and (contains-tags tags :strong-key :weak-key)
					 (not (contains-tags (second (sp-next context)) :enumerated :flag)))
		[(conj result [field (first (sp-next context))]) (choose-field-type context field :key)]
		[result (sp-skip context)]))
;end tagging

;TODO refactor
(defn build-arg-pairs [args schema]
	(let [field-keys (get-field-keys schema)
				layout-to-field-key-map
				(specter/multi-transform (specter/multi-path
																	 [specter/MAP-VALS specter/ALL (specter/terminal #(-> [(:key %) (:args (get-layout-details %))]))]
																	 [specter/MAP-VALS (specter/terminal util/map-convert-pairs)])
																 (group-fields-by-layouts schema))]
		(reduce (fn [context val]
							(let [previous-result (first context)]
								(cond
									(= ::key (first previous-result))
										(conj (rest context) [(second previous-result) val])
									(contains? (:default layout-to-field-key-map) val)
										(conj context [::key val])
									(contains? (:flag layout-to-field-key-map) val)
										(conj context [val true])
									:else (let [potential-enumerated-val
															(util/find-first (fn [entry] (contains? (first (second entry)) val))
																					(specter/select [:enumerated specter/ALL] layout-to-field-key-map))]
													(cond
														(some? potential-enumerated-val)
															(conj context [(first potential-enumerated-val) val])
														(contains? field-keys val)
															(conj context [::key val])
														:else
															(conj context [(first (keys (:primary layout-to-field-key-map))) val])))))) util/concrete-seq args)))

(defn schema-args-to-map [args schema] (util/map-convert-pairs (build-arg-pairs args schema)))

(defn build-defaults-map-from-schema-fields [fields arg-map]
	(util/map-convert-pairs (map #(cond
											 (some? (:default %)) [(:key %) (:default %)]
											 (some? (:default-fn %)) [(:key %) ((:default-fn %) arg-map)]
											 :else nil) fields)))

(defn normalize-schema-tag [schema-tag]
	(cond
		(nil? schema-tag) nil
		(vector? schema-tag) schema-tag
		:else [:default schema-tag]))

(defn apply-defaults-shallow [arg-map schema]
	(merge arg-map
				 (build-defaults-map-from-schema-fields
					 (filter-fields #(not (contains? arg-map (:key %))) schema)
					 arg-map)))

(defn apply-defaults [arg-map schema]
	(let [default-map-pairs (seq (apply-defaults-shallow arg-map schema))]
		(util/map-convert-pairs (map (fn [[key value]]
												 (let [sub-schema-tag (normalize-schema-tag (:schema (get-field key schema)))]
													 (cond
														 (empty? sub-schema-tag)
														 [key value]
														 (= :collection (first sub-schema-tag))
														 [key (util/to-vec (map #(apply-defaults % (second sub-schema-tag)) value))]
														 :else
														 [key (apply-defaults value (second sub-schema-tag))])))
																 default-map-pairs))))

(defn mapify-shallow [args schema]
	(if (map? args) args (schema-args-to-map args schema)))

;TODO don't implement using recursion
(defn mapify [args schema]
	(let [reified-map-pairs (seq (mapify-shallow args schema))]
		(util/map-convert-pairs (map (fn [[key value]]
												 (let [sub-schema-tag (normalize-schema-tag (:schema (get-field key schema)))]
													 (cond
														 (empty? sub-schema-tag)
														 [key value]
														 (= :collection (first sub-schema-tag))
														 [key (util/to-vec (map #(mapify % (second sub-schema-tag)) value))]
														 :else
														 [key (mapify value (second sub-schema-tag))])))
																 reified-map-pairs))))

(defn mapify-args [args schema]
	(if (and (= (count args) 1)
					 (or (map? (first args)) (vector? (first args))))
		(mapify (first args) schema)
		(mapify args schema)))

(defn substantiate-map [mapified-args schema] (apply-defaults mapified-args schema))
(defn substantiate [args schema] (substantiate-map (mapify args schema) schema))

(defn substantiate-schema [schema & inherits]
	(let [substantiated-schema (substantiate schema schema-schema)]
		(assoc substantiated-schema :fields (util/concat-vec (util/flatten-1 (map :fields inherits)) (:fields substantiated-schema)))))

(def default-hooks
	{:default-override (util/static-fn {})
	 })

(defn make-fn-scaffolding
	([schema f]
	 (make-fn-scaffolding schema f default-hooks))

	([schema f {:keys [default-override] :as hooks}]
	 (fn [& args]
		 (let [mapified-args (mapify-args args schema)
					 reified-args (substantiate-map
													(merge mapified-args (default-override mapified-args))
													schema)]
			 (timbre/trace
				 "schema/make-fn with schema:" schema "Reified args" reified-args "from actual args" args)
			 (f mapified-args reified-args)))))

(defn schema-handler [schema f] (make-fn-scaffolding schema (fn [_ reified-args] (f reified-args))))

(defn build-default-overrides [current-args previous-args previous-reified-args]
	(util/map-convert-pairs (util/remove-nil (map
															#(let [current-val (get current-args %)
																		 prev-val (get previous-args %)]
																(if (= current-val prev-val)
																	[% (get previous-reified-args %)]
																	nil))
															(keys previous-reified-args)))))

(defn make-fn [schema f]
	(make-fn-scaffolding schema (fn [_ reified-args] (f reified-args))))

(def component-handler-schema
	(substantiate-schema [::component-handler
												:fields [[:key :schema]
																 [:initializer :default (util/static-fn {})]
																 [:template :layout-type :primary]
																 [:static :layout-type :flag :default false]]]))

(def component-handler
	(schema-handler component-handler-schema
									(fn [{:keys [schema initializer template static]}]
										(if static
											(make-fn-scaffolding schema
																					 (fn [_ initial-args]
																						 (let [initializer-results (initializer initial-args)]
																							 (make-fn-scaffolding schema
																																		(fn [_ actual-args]
																																			(template (merge actual-args initializer-results)))))))
											(make-fn-scaffolding schema
																					 (fn [mapified-args initial-args]
																						 (let [initializer-results (initializer initial-args)
																									 previous-args-atom (atom [mapified-args initial-args])]
																							 (make-fn-scaffolding schema
																																		(fn [actual-mapified-args actual-args]
																																			(reset! previous-args-atom [actual-mapified-args actual-args])
																																			(template (merge actual-args initializer-results)))
																																		{:default-override
																																		 (fn [mapified-args]
																																			 (let [[previous-args previous-reified-args] (deref previous-args-atom)]
																																				 (build-default-overrides mapified-args previous-args previous-reified-args)))
																																		 }))))))))

(def example-component-schema
	(substantiate-schema [::example-component
												:fields [[:key :primary]
																 [:title]
																 [:example]
																 [:source]
																 [:additional-content]]]))
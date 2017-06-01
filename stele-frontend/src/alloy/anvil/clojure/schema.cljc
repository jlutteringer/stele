(ns alloy.anvil.clojure.schema
  (:require [alloy.anvil.clojure.util :as util]
            [taoensso.timbre :as timbre #?@(:cljs (:include-macros true))]
            [com.rpl.specter :as specter #?@(:cljs (:include-macros true))]
            [clojure.spec :as spec #?@(:cljs (:include-macros true))]))

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

(spec/def ::schema-field.layout-type schema-layout-types)

(def schema-field-schema
  {:key         :schema-field
   :name        "Schema Field"
   :description "This is a description"
   :fields      [{:key :key :description "The unique (per schema) key of the schema field" :layout-type :primary :required true :spec ::util/keyword}
                 {:key :description :description "The description of the schema field" :layout-type :default :required false :spec ::util/string}
                 {:key :layout-type :description "The layout of the schema field for args coercion" :layout-type [:enumerated schema-layout-types] :default :default :required true :spec ::schema-field.layout-type}
                 {:key :default :description "The default value for args coercion" :layout-type :default :required false}
                 {:key :default-fn :description "The default fn to be ran at the time of args coercion" :layout-type :default :required false :spec ::util/fn}
                 {:key :schema :description "The schema type of the field" :layout-type :default :required false :spec ::util/map}
                 {:key :required :description "The schema type of the field" :layout-type :flag :default false :required false :spec ::util/boolean}
                 {:key :spec :description "The schema type of the field" :layout-type :default :required false :spec ::util/keyword :conform-fn (fn [spec] spec)}
                 {:key :conform-fn :description "The schema type of the field" :layout-type :default :required false :spec ::util/fn}]})

(def schema-schema
  {:key         ::schema
   :name        "Schema"
   :description "This is a description"
   :fields      [{:key :key :description "The key of the schema" :layout-type :primary :required true :spec ::util/keyword}
                 {:key :name :description "The name of the schema" :layout-type :default :default-fn #(name (:key %)) :required true :spec ::util/string}
                 {:key :description :description "The description of the schema" :layout-type :default :required false :spec ::util/string}
                 {:key :fields :description "The fields of the schema" :layout-type :default :schema [:collection schema-field-schema] :required true :spec ::util/vector}
                 {:key :spec :description "The spec of the schema" :layout-type :default :required false :spec ::util/keyword :conform-fn (fn [spec] spec)}]})

(defn group-fields-by-layouts [schema]
  (util/filter-groups schema-layout-types #(filter-fields-by-layout % schema)))

(defn tag-args [args schema]
  (let [field-keys (get-field-keys schema)
        layout-to-field-map
        (specter/multi-transform (specter/multi-path
                                   [specter/MAP-VALS specter/ALL (specter/terminal #(-> [(:key %) %]))]
                                   [specter/MAP-VALS (specter/terminal util/map-from-pairs)])
                                 (group-fields-by-layouts schema))
        {:keys [default]} layout-to-field-map]
    (util/fmap-contextual
      (fn [arg context]
        (let [arg-field (get-field arg schema)
              previous-context (second (first context))
              potential-enumerated-val
              (util/find-first (fn [entry] (contains? (second (:layout-type (second entry))) arg))
                               (specter/select [:enumerated specter/ALL] layout-to-field-map))]
          [arg (util/->map-from-pairs
                 (when (contains? previous-context :weak-key)
                   [:value (:weak-key previous-context)])
                 (when (contains? previous-context :strong-key)
                   [:value (:strong-key previous-context)])
                 (when (and (empty? context) (util/not-empty? (:primary layout-to-field-map)))
                   [:primary (get-field (first (keys (:primary layout-to-field-map))) schema)])
                 (cond
                   (contains? default arg) [:strong-key arg-field]
                   (contains? field-keys arg) [:weak-key arg-field])
                 (when (contains? (:flag layout-to-field-map) arg)
                   [:flag arg-field])
                 (when (some? potential-enumerated-val)
                   [:enumerated (get-field (first potential-enumerated-val) schema)]))]))
      args)))

(defn build-token-select-transformations-internal [token type seq]
  (println "build select paths")
  (println token)
  (println type)
  (println seq)
  (let [token-pred #(= (first %) token)
        token-query (specter/filterer token-pred)
        concrete-token (specter/select-one [token-query specter/FIRST] seq)]
    (util/->vec
      (when (contains? (second concrete-token) :value)
        [(specter/srange-dynamic
           (fn [form] (dec (util/find-index token-pred form)))
           (fn [form] (util/find-index token-pred form)))
         specter/FIRST
         specter/LAST
         (specter/filterer #(contains? #{:strong-key :weak-key :key} (first %)))
         (specter/terminal specter/NONE)])

      [[token-query (specter/terminal specter/NONE)]]

      (when (= :primary type)
        [specter/ALL specter/LAST (specter/filterer (fn [item] (= (first item) :primary))) (specter/terminal specter/NONE)])

      ;remove all tags referring to the token
      [specter/ALL specter/LAST (specter/filterer (fn [item] (= (:key (second item)) (first token)))) (specter/terminal specter/NONE)]

      ;clean up some of our nulls that we leave around
      [specter/ALL specter/LAST (specter/terminal (fn [m] (util/map-from-pairs (filter #(or (some? (first %)) (some? (second %))) m))))]
      )))

(defn build-token-select-transformations [token type seq context]
  (if (contains? #{:strong-key :weak-key :key} type)
    (util/concat-vec (build-token-select-transformations-internal token type seq)
                     (build-token-select-transformations-internal (first (util/get-next context)) :value seq))
    (build-token-select-transformations-internal token type seq)))

; this could def be more efficient
(defn select-transform [token type seq context]
  (util/specter-loop-transform (build-token-select-transformations token type seq context) seq))

(defn select-token [token type seq context]
  (util/debug (filter some? (select-transform token type seq context))))

(defn get-token-value [token type context]
  (cond
    (contains? #{:strong-key :weak-key :key} type) (first (first (util/get-next context)))
    (= type :flag) true
    :else (first (first token))))

(defn finalize-token [type context]
  (let [token (util/get-current context)
        value (get-token-value token type context)
        target-seq (second context)]

    (util/append-result [(-> token second type :key) value]
                        (select-token (first token) type target-seq context)
                        context)))

(defn gather-must-pick-options [token context] [])

(defn general-element-processor [[[token _] tags] context]
  (cond
    (= 0 (count (util/multimap-vals tags))) (util/halting-error (str "No availabile tags for token " token " halting processing"))
    (= 1 (count tags)) (let [[tag _] (first tags)]
                         (finalize-token tag context))
    :else
    (let [must-pick-options (gather-must-pick-options token context)]
      (cond
        (< 1 (count must-pick-options)) (util/halting-error (str "We have more than one 'must pick' option in options " must-pick-options))
        (= 1 (count must-pick-options)) (let [[tag _] (first must-pick-options)]
                                          (finalize-token tag context))
        :else (util/skip context)))))

(defn predictive-element-processor [[[token _] tags] context] )

(defn finalizing-tag-processor [tag]
  (fn [[_ tags] context]
    (if (contains? tags tag)
      (finalize-token tag context)
      (util/skip context))))

(defn key-tag-processor [[_ tags] context]
  (if (util/contains-any? tags #{:strong-key :weak-key :key})
    (finalize-token (util/contained-val tags #{:strong-key :weak-key :key}) context)
    (util/skip context)))

(def parse-tags-pipeline (util/iteration-process
                           (util/element-processor
                             general-element-processor
                             key-tag-processor
                             (finalizing-tag-processor :flag)
                             (finalizing-tag-processor :enumerated))))

(defn parse-tags [tagged-args]
  (let [unique-tags (specter/transform [specter/ALL specter/FIRST] #(-> [% (util/rand-uuid)]) tagged-args)]
    (parse-tags-pipeline unique-tags [])))

(defn schema-args-to-map-v2 [args schema] (parse-tags (tag-args args schema)))

;end tagging

;TODO refactor
(defn build-arg-pairs [args schema]
  (let [field-keys (get-field-keys schema)
        layout-to-field-key-map
        (specter/multi-transform (specter/multi-path
                                   [specter/MAP-VALS specter/ALL (specter/terminal #(-> [(:key %) (:args (get-layout-details %))]))]
                                   [specter/MAP-VALS (specter/terminal util/map-from-pairs)])
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

(defn schema-args-to-map [args schema] (util/map-from-pairs (build-arg-pairs args schema)))

(defn build-defaults-map-from-schema-fields [fields arg-map]
  (util/map-from-pairs (map #(cond
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
    (util/map-from-pairs (map (fn [[key value]]
                                (let [sub-schema-tag (normalize-schema-tag (:schema (get-field key schema)))]
                                  (cond
                                    (empty? sub-schema-tag)
                                    [key value]
                                    (= :collection (first sub-schema-tag))
                                    [key (vec (map #(apply-defaults % (second sub-schema-tag)) value))]
                                    :else
                                    [key (apply-defaults value (second sub-schema-tag))])))
                              default-map-pairs))))

(defn mapify-shallow [args schema]
  (if (map? args) args (schema-args-to-map args schema)))

;TODO don't implement using recursion
(defn mapify [args schema]
  (let [reified-map-pairs (seq (mapify-shallow args schema))]
    (util/map-from-pairs (map (fn [[key value]]
                                (let [sub-schema-tag (normalize-schema-tag (:schema (get-field key schema)))]
                                  (cond
                                    (empty? sub-schema-tag)
                                    [key value]
                                    (= :collection (first sub-schema-tag))
                                    [key (vec (map #(mapify % (second sub-schema-tag)) value))]
                                    :else
                                    [key (mapify value (second sub-schema-tag))])))
                              reified-map-pairs))))

(defn mapify-args [args schema]
  (if (and (= (count args) 1)
           (or (map? (first args))))
    (first args)
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
  (util/map-from-pairs (util/remove-nil (map
                                          #(let [current-val (get current-args %)
                                                 prev-val (get previous-args %)]
                                            (if (= current-val prev-val)
                                              [% (get previous-reified-args %)]
                                              nil))
                                          (keys previous-reified-args)))))

(defn make-fn [schema f]
  (make-fn-scaffolding schema (fn [_ reified-args] (f reified-args))))

(defn make-constructor [schema]
  (make-fn-scaffolding schema (fn [_ reified-args] reified-args)))

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
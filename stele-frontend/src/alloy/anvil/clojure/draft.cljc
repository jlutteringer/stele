(ns alloy.anvil.clojure.draft
  (:require [alloy.anvil.clojure.util :as util]
            [taoensso.timbre :as timbre #?@(:cljs (:include-macros true))]
            [com.rpl.specter :as specter #?@(:cljs (:include-macros true))]))

(def draft-layout-types #{::layout-default ::layout-primary ::layout-enumerated ::layout-flag})

(def draft-field-schema
  {::key         ::draft-field
   ::name        "Draft Field"
   ::description "This is a description"
   ::fields      [{::key ::key ::description "The unique (per schema) key of the schema field" ::layout-type ::layout-primary}
                  {::key ::description ::description "The description of the schema field" ::layout-type ::layout-default}
                  {::key ::layout-type ::description "The layout of the schema field for args coercion" ::layout-type [::layout-enumerated draft-layout-types] ::default-val ::default}
                  {::key ::default-val ::description "The default value for args coercion" ::layout-type ::layout-default ::required false}
                  {::key ::default-fn ::description "The default fn to be ran at the time of args coercion" ::layout-type ::layout-default ::required false}
                  {::key ::type ::description "The type of the field" ::layout-type ::layout-default ::required false}]})

(def draft-schema
  {::key         ::draft
   ::name        "Draft"
   ::description "This is a description"
   ::fields      [{::key ::key ::description "The key of the schema" ::layout-type ::layout-primary}
                  {::key ::name ::description "The name of the schema" ::layout-type ::layout-default ::default-fn #(name (::key %))}
                  {::key ::description ::description "The description of the schema" ::layout-type ::layout-default}
                  {::key ::fields ::description "The fields of the schema" ::layout-type ::layout-primary ::type [::draft-field]}]})

(defn get-layout-details [field]
  (let [layout-type (::layout-type field)]
    (if (coll? layout-type)
      {:type (first layout-type) :args (rest layout-type)}
      {:type layout-type :args []})))

(defn filter-fields [pred schema] (filter pred (::fields schema)))

(defn get-field [key schema] (first (filter-fields #(= key (::key %)) schema)))

(defn get-field-keys [schema] (util/to-set (map ::key (::fields schema))))

(defn filter-fields-by-layout [layout schema]
  (filter-fields #(= layout (:type (get-layout-details %))) schema))

(defn group-fields-by-layouts [schema]
  (util/filter-groups draft-layout-types #(filter-fields-by-layout % schema)))

(defn tag-args [args schema]
  (let [field-keys (get-field-keys schema)
        layout-to-field-map
        (specter/multi-transform (specter/multi-path
                                   [specter/MAP-VALS specter/ALL (specter/terminal #(-> [(::key %) %]))]
                                   [specter/MAP-VALS (specter/terminal util/map-from-pairs)])
                                 (group-fields-by-layouts schema))]
    (util/fmap-contextual
      (fn [arg context]
        (let [arg-field (get-field arg schema)
              previous-context (second (first context))
              potential-enumerated-val
              (util/find-first (fn [entry] (contains? (second (::layout-type (second entry))) arg))
                               (specter/select [::layout-enumerated specter/ALL] layout-to-field-map))
              tagged-value
              [arg (util/->map-from-pairs
                     (when (contains? previous-context :key)
                       [:value (:key previous-context)])
                     (when (util/not-empty? (::layout-primary layout-to-field-map))
                       [:primary (get-field (first (keys (::layout-primary layout-to-field-map))) schema)])
                     (when (contains? field-keys arg)
                       [:key arg-field])
                     (when (contains? (::layout-flag layout-to-field-map) arg)
                       [:flag arg-field])
                     (when (some? potential-enumerated-val)
                       [:enumerated (get-field (first potential-enumerated-val) schema)]))]]

          (timbre/trace tagged-value)
          tagged-value))
      args)))

(defn build-token-select-transformations-internal [token type seq]
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
         (specter/filterer #(= :key (first %)))
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
  (util/concat-vec (build-token-select-transformations-internal token type seq)
                   (when (= type :key)
                     (build-token-select-transformations-internal (first (util/get-next context)) :value seq))))

(defn select-transform [token type seq context]
  (util/specter-loop-transform (build-token-select-transformations token type seq context) seq))

(defn select-token [token type seq context]
  (filter some? (select-transform token type seq context)))

(defn get-token-value [token type context]
  (cond
    (= :key type) (first (first (util/get-next context)))
    (= type :flag) true
    :else (first (first token))))

(defn finalize-token [type context]
  (let [token (util/get-current context)
        value (get-token-value token type context)
        target-seq (second context)]

    (util/append-result [(-> token second type ::key) value]
                        (select-token (first token) type target-seq context)
                        context)))

(defn gather-must-pick-options [token context] [])

(defn general-element-processor [[[token _] tags] context]
  (cond
    (= 0 (count (util/multimap-vals tags))) (util/halting-error (str "No availabile tags for token " token " halting processing"))
    (= 1 (count tags)) (let [[tag _] (first tags)] (finalize-token tag context))
    :else
    (let [must-pick-options (gather-must-pick-options token context)]
      (cond
        (< 1 (count must-pick-options)) (util/halting-error (str "We have more than one 'must pick' option in options " must-pick-options))
        (= 1 (count must-pick-options)) (let [[tag _] (first must-pick-options)]
                                          (finalize-token tag context))
        :else (util/skip context)))))

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

(defn parse-args [args schema] (parse-tags (tag-args args schema)))
(defn coerce [args schema] (util/map-from-pairs (parse-args args schema)))
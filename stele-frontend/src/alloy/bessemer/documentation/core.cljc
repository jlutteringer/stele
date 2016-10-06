(ns alloy.bessemer.documentation.core
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.anvil.clojure.schema :as schema]
						[com.rpl.specter :as specter #?@(:cljs (:include-macros true))]))

(def example-component-schema
	(schema/substantiate-schema [::example-component
															 :fields [[:example :primary]
																				[:key :description]
																				[:source]]]))

(def example-section-schema
	(schema/substantiate-schema [::example-component
															 :fields [[:key :key :primary]
																				[:title]
																				[:key :description]
																				[:examples [:collection example-component-schema]]
																				[:additional-content]]]))
(def sub-section-schema
	(schema/substantiate-schema [::sub-section
															 :fields [[:key :key :primary]
																				[:title]
																				[:key :description]
																				[:components :schema [:collection example-section-schema]]]]))

(def section-schema
	(schema/substantiate-schema [::section
															 :fields [[:key :key :primary]
																				[:title]
																				[:key :description]
																				[:sub-sections :schema [:collection sub-section-schema]]]]))

(defn empty-registry [] {:targets {:section-key (atom nil)
																	 :sub-section-key (atom nil)
																	 :example-section-key (atom nil)}
												 :registry (atom {})})

(defonce global-section-registry (empty-registry))

(defn register-example-component [registry example]
	(reset! (:registry registry)
					(specter/transform [@(-> registry :targets :section-key)
															:sub-sections
															(specter/filterer #(= (:key %) @(-> registry :targets :sub-section-key)))
															specter/FIRST
															:components
															(specter/filterer #(= (:key %) @(-> registry :targets :example-section-key)))
															specter/FIRST
															:examples] (fn [x] (util/concat-vec x example)) @(:registry registry))))

(defn register-example-section [registry example]
	(reset! (:registry registry)
					(specter/transform [@(-> registry :targets :section-key)
															:sub-sections
															(specter/filterer #(= (:key %) @(-> registry :targets :sub-section-key)))
															specter/FIRST
															:components] (fn [x] (util/concat-vec x example)) @(:registry registry)))
	(reset! (-> registry :targets :example-section-key) (:key example)))

(defn register-sub-section [registry sub-section]
	(reset! (-> registry :registry)
					(specter/transform [@(-> registry :targets :section-key)
															:sub-sections] (fn [x] (util/concat-vec x sub-section)) @(:registry registry)))
	(reset! (-> registry :targets :sub-section-key) (:key sub-section)))

(defn register-section [registry key section]
	(reset! (-> registry :registry) (assoc @(:registry registry) key section))
	(reset! (-> registry :targets :section-key) key))

(def add-example
	(schema/make-fn example-component-schema
									(fn [example]
										(register-example-component global-section-registry example))))

(def def-example
	(schema/make-fn example-section-schema
									(fn [example]
										(register-example-section global-section-registry example))))

(def def-sub-section
	(schema/make-fn sub-section-schema
									(fn [sub-section]
										(register-sub-section global-section-registry sub-section))))

(def schema-section-schema
	(schema/substantiate-schema [::schema-section
															 :fields [[:key :schema :primary]
																				[:key :description]]]))

(def def-schema-section
	(schema/make-fn schema-section-schema
									(fn [{:keys [schema description]}]
										(def-sub-section (:key schema) :title (:name schema) :description description))))

(def def-section
	(schema/make-fn section-schema
									(fn [section]
										(register-section global-section-registry :fake-key section))))

(defn sections [registry & namespaces] (vals @(:registry registry)))
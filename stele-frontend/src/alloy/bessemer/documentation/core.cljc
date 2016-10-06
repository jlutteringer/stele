(ns alloy.bessemer.documentation.core
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.anvil.clojure.schema :as schema]
						[com.rpl.specter :as specter #?@(:cljs (:include-macros true))]))

(def example-component-schema
	(schema/substantiate-schema [::example-component
															 :fields [[:key :key :primary]
																				[:title]
																				[:key :description]
																				[:example]
																				[:source]
																				[:additional-content]]]))
(def sub-section-schema
	(schema/substantiate-schema [::sub-section
															 :fields [[:key :key :primary]
																				[:title]
																				[:key :description]
																				[:components :schema [:collection example-component-schema]]]]))

(def section-schema
	(schema/substantiate-schema [::section
															 :fields [[:key :key :primary]
																				[:title]
																				[:key :description]
																				[:sub-sections :schema [:collection sub-section-schema]]]]))

(defn empty-registry [] {:targets {:section-key (atom nil)
																	 :sub-section-key (atom nil)}
												 :registry (atom {})})

(def global-section-registry (empty-registry))

(defn register-example [registry example]
	(reset! (:registry registry)
					(specter/transform [@(-> registry :targets :section-key)
															:sub-sections
															(specter/filterer #(= (:key %) @(-> registry :targets :sub-section-key)))
															specter/FIRST
															:components] (fn [x] (util/concat-vec x example)) @(:registry registry))))

(defn register-sub-section [registry sub-section]
	(reset! (:registry registry)
					(specter/transform [@(-> registry :targets :section-key)
															:sub-sections] (fn [x] (util/concat-vec x sub-section)) @(:registry registry)))
	(reset! (-> registry :targets :sub-section-key) (:key sub-section)))

(defn register-section [registry key section]
	(reset! (-> registry :registry) (assoc @(:registry registry) key section))
	(reset! (-> registry :targets :section-key) key))

(defn example-component [form] (schema/substantiate form example-component-schema))
(defn sub-section [form] (schema/substantiate form sub-section-schema))
(defn section [form] (schema/substantiate form section-schema))

(def def-example
	(schema/make-fn example-component-schema
									(fn [example] (register-example global-section-registry example))))

(def def-sub-section
	(schema/make-fn sub-section-schema
									(fn [sub-section] (register-sub-section global-section-registry sub-section))))

(def def-section
	(schema/make-fn section-schema
									(fn [section] (register-section global-section-registry :fake-key section))))

(defn sections [registry & namespaces] (util/debug @(:registry registry)))
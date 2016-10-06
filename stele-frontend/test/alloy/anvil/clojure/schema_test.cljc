(ns alloy.anvil.clojure.schema-test
	(:require [clojure.test :as test]
						[alloy.anvil.clojure.schema :as schema]))

(test/is
	(=
		(schema/substantiate-schema [::example-component
																:fields [[:key :primary]
																				 [:title]
																				 [:example]
																				 [:source]
																				 [:additional-content]]])
		{:key ::example-component
		 :name "example-component"
		 :fields [{:key :key :layout-type :primary}
							{:key :title :layout-type :default}
							{:key :example :layout-type :default}
							{:key :source :layout-type :default}
							{:key :additional-content :layout-type :default}]}))

(test/is
	(=
		(schema/substantiate [:schema :primary] schema/schema-field-schema)
		{:key :schema :layout-type :primary}))
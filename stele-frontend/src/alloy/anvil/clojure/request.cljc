(ns alloy.anvil.clojure.request
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.anvil.clojure.schema :as schema]
						[clojure.string :as string]))

(defrecord RequestUri [path parameters hash extension])

(def uri-schema
	(schema/reify-schema ["RequestUri"
											:fields [[:path]
															 [:parameters]
															 [:hash]
															 [:extension]]]))

(defn uri-stringify [& {:keys [path extenstion parameters]}]
	(str
		(str "/" (string/join "/" path))
		(when (some? extenstion) (str "." extenstion))
		(when (util/not-empty? parameters)
			(str "?" (string/join "&" (map (fn [[key val]] (str (name key) "=" val)) parameters))))))

(def uri
	{:path ["crm" "test" "generate"]
	:parameters
	{:key "value"
	 :key2 "value"}
	:extension "sm"})

(def server
	{:id "localhost"
	 :port 80})

(def url-request
	{:server server
	 :uri uri
	 :method :get

	 })
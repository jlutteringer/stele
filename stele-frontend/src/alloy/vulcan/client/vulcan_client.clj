(ns alloy.vulcan.client.vulcan-client
  (:require [clojure.spec :as spec]
            [alloy.anvil.clojure.util :as util]
            [alloy.anvil.clojure.glob :as glob]))

(defrecord VulcanProperty [key spec default-value property-source])

(defprotocol PropertySource
  (get-properties [_ keys app-details]))

(defn get-property [property-source key app-details]
  (let [property-map (get-properties property-source [key] app-details)]
    (if (empty? property-map)
      ::property-miss
      (first (vals property-map)))))

(defn property-miss? [val] (= val ::property-miss))

(defn resolve-property-from-source [key property-source app-details]
  (get-properties property-source [key] app-details))

(defn resolve-property-from-sources [key property-sources app-details]
  (loop [property-source & remaining-sources property-sources]
    (if (some? property-source)
      (let [property-val (resolve-property-from-source key property-source app-details)]
        (if (property-miss? property-val)
          (recur remaining-sources)
          property-val))

      ;we return property miss if we can't find a resolution
      ::property-miss)))

(defn resolve-properties-from-sources [keys property-sources app-details]
  (util/map-from-fmap-filter
    #(not (property-miss? (second %)))
    #(-> [% (resolve-property-from-sources % property-sources app-details)])
    keys))

(defrecord ChainPropertySource [property-sources]
  PropertySource
  (get-properties [_ keys app-details] (resolve-properties-from-sources keys property-sources app-details)))

(defrecord MapPropertySource [map-source]
  PropertySource
  (get-properties [_ keys app-details] (select-keys (util/valueate map-source) keys)))

(defrecord JavaPropertySource [properties-source]
  PropertySource
  (get-properties [_ keys app-details]
    (util/map-from-fmap-filter
      #(some? (second %))
      #(-> [% (.getProperty (util/valueate properties-source) (name %))])
      keys)))

;todo
(defrecord GlobPropertySource [glob]
  (glob/glob glob))

(def system-property-source (->JavaPropertySource #(System/getProperties)))
(def environment-property-source (->MapPropertySource #(System/getenv)))

(defn resolve-property [{:keys [key spec default-value property-source]} app-details]
  (let [resolved-property (get-property property-source key app-details)]
    (if (property-miss? resolved-property)
      default-value
      (spec/conform spec resolved-property))))

(defn resolve-properties [properties app-details] )
(ns alloy.stele.frontend.reagent-util
	(:require [alloy.anvil.clojure.util :as util]))

(defmacro build-component-body
	([arg-symbol schema handler-template]
	 (util/build-handler-fn-body-internal arg-symbol schema handler-template))
	([arg-symbol schema initializer-template handler-template]
	 (util/build-handler-fn-body-internal arg-symbol schema initializer-template handler-template)))

(defmacro build-component
	([handler-template]
	 (util/build-handler-fn-internal handler-template))
	([schema handler-template]
	 (util/build-handler-fn-internal schema handler-template))
	([schema initializer-template handler-template]
	 (util/build-handler-fn-internal schema initializer-template handler-template)))
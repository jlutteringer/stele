(ns alloy.anvil.clojure.math
  (:require [alloy.anvil.clojure.util :as util]))

(defn cieling [num] #?(:clj  (Math/ceil num)
                       :cljs (.ceil js/Math num)))

(defn point-add [& points] (apply (partial util/fmap-longest + 0) points))

(defn points-between [[ax ay] [bx by]] (mapcat (fn [x] (map (fn [y] [x y]) (range ay (inc by)))) (range ax (inc bx))))
(defn point-min [[ax ay] [bx by]] [(min ax bx) (min ay by)])
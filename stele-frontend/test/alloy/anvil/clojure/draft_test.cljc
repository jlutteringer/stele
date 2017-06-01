(ns alloy.anvil.clojure.draft-test
  (:require [alloy.anvil.clojure.draft :as draft]
            [clojure.test :as test]))

(def examples
  [{:schema draft/draft-field-schema
    :draft [:dropdown ::draft/description "This is your standard dropdown list" ::draft/layout-primary ::draft/default-val "Dropdown Default Value"]
    :tags [[:dropdown {:primary (draft/get-field ::draft/key draft/draft-field-schema)}]
           [::draft/description {:key (draft/get-field ::draft/description draft/draft-field-schema)}]
           ["This is your standard dropdown list" {:value (draft/get-field ::draft/description draft/draft-field-schema)}]
           [::draft/layout-primary {:enumerated (draft/get-field ::draft/layout-type draft/draft-field-schema)}]
           [::draft/default-val {:key (draft/get-field ::draft/default-val draft/draft-field-schema)}]
           ["Dropdown Default Value" {:value (draft/get-field ::draft/default-val draft/draft-field-schema)}]]
    :result {::draft/key :dropdown ::draft/description "This is your standard dropdown list" ::draft/layout-type ::draft/primary ::draft/default-val "Dropdown Default Value"}}
   {:schema draft/draft-schema
    :draft [:panel ::draft/name "Panel" ::draft/description "This is a panel"
            ::draft/fields [[:dropdown ::draft/description "This is your standard dropdown list" ::draft/layout-primary ::draft/default-val "Dropdown Default Value"]]]
    :tags []
    :result {}}])

(test/testing "Draft"
  (test/testing "argument tagging"
    (doall (map #(test/is (:tags %) (draft/tag-args % (:schema %))) examples))
    (test/is (= 7 (+ 3 4))))
  (test/testing "argument tagging"
    (doall (map #(test/is (:tags %) (draft/tag-args % (:schema %))) examples))
    (test/is (= 7 (+ 3 4)))))
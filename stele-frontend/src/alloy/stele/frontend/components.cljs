(ns alloy.stele.frontend.components
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.stele.frontend.components.dropdown :as dropdown]
						[reagent.core :as reagent]
						[reagent.impl.template :as r-template]
						[re-frame.core :as re-frame]
						[re-com.core :as re-com]))

;dropdown with arbitrary content
(defn test-dropdown []
	[dropdown/dropdown
	 :label "Select"
	 :content [[:div "This is arbitrary content"]]])

(defn test-dropdown2 []
	[dropdown/select
	 :choices [{:id "test" :label "Test"} {:id "test2" :label [:a "Test 2Test 2Test 2Test 2Test 2Test 2Test 2Test 2"]}]
	 :model nil
	 :placeholder "Select"
	 :on-change #(js/console.log "changed")])

(defn test-dropdown3 []
	[dropdown/select
	 :choices [{:id "test" :label "Test"} {:id "test2" :label [:a "Test 2Test 2Test 2Test 2Test 2Test 2Test 2Test 2"]}]
	 :model nil
	 :placeholder "Select"
	 :on-change #(js/console.log "changed")])

(defn as-form [element] (if (fn? element) (element) element))

(defn as-element [element]
	(r-template/as-element (as-form element)))

(defn nav-dropdown [_] (fn [dropdown] [:li (as-form dropdown)]))

(defn nav-bar []
	[:nav {:class "navbar navbar-default"}
	 [:ul {:class "nav navbar-nav"}
		[:li [:a "Link"]]
		[:li [:a "Link"]]
		[:li [:a "Link"]]
		[nav-dropdown test-dropdown]
		[nav-dropdown test-dropdown2]
		[nav-dropdown test-dropdown3]]])

;[:div {:class "navbar navbar-default"}
; [:div {:class "navbar-header"}
;	[:a {:class "navbar-brand"} "Stele"]]
; [:ul {:class "nav navbar-nav"}
;	[:li [:a "Link"]]
;	[:li [:a "Link"]]
;	[:li [:a "Link"]]
;	[:li [re-com/single-dropdown
;				:choices [{:id "test" :label "Test"} {:id "test2" :label "Test 2"}]
;				:model nil
;				:placeholder "Select"
;				:on-change #(js/console.log "changed")]]]]
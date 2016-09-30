(ns alloy.stele.frontend.components
	(:require [alloy.anvil.clojure.util :as util]
						[alloy.bessemer.components.dropdown :as dropdown]
						[reagent.core :as reagent]
						[reagent.impl.template :as r-template]
						[re-frame.core :as re-frame]
						[re-com.core :as re-com]))

;dropdown with arbitrary content
(defn test-dropdown []
	[dropdown/dropdown
	 :label "Dropdown"
	 :content "This can be arbitrary content"])

(defn test-dropdown-list []
	[dropdown/dropdown-list
	 :label "Dropdown List"
	 :choices [{:id "item1" :label "Item 1" :on-click #(print "clicked")} {:id "item2" :label [:a "Item 2"]}]
	 :open-state (reagent/atom false)])

(defn test-select []
	[dropdown/select
	 :placeholder "Select"
	 :choices [{:id "item1" :label "Item 1"} {:id "item2" :label [:a "Item 2"]}]
	 :open-state (reagent/atom false)])

(defn test-dropdown2 []
	[dropdown/select-old
	 :choices [{:id "test" :label "Test"} {:id "test2" :label [:a "Test 2Test 2Test 2Test 2Test 2Test 2Test 2Test 2"]}]
	 :model nil
	 :placeholder "Select"
	 :on-change #(js/console.log "changed")])

(defn test-dropdown3 []
	[dropdown/dropdown
	 :label "Label"
	 :content "Temp Content"
	 :open-state (reagent/atom false)])

(defn as-form [element] (if (fn? element) (element) element))

(defn as-element [element]
	(r-template/as-element (as-form element)))

(defn nav-dropdown [_] (fn [dropdown] [:li (as-form dropdown)]))

(defn nav-bar []
	[:nav {:class "navbar navbar-default"}
	 [:ul {:class "nav navbar-nav"}
		[nav-dropdown test-dropdown]
		[nav-dropdown test-dropdown-list]
		[nav-dropdown test-select]]])

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
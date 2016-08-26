(ns alloy.stele.frontend.app
	(:require [alloy.stele.frontend.components :as components]
						[reagent.core :as reagent]
						[re-frame.core :as re-frame]
						[re-com.core :as re-com])
	(:require-macros [mount.core :refer [defstate]]
									 [reagent.ratom :refer [reaction]]))

(defn test-dropdown []
	[re-com/single-dropdown
	 :choices [{:id "test" :label "Test"} {:id "test2" :label [:a "Test 2Test 2Test 2Test 2Test 2Test 2Test 2Test 2"]}]
	 :model nil
	 :placeholder "Select"
	 :on-change #(js/console.log "changed")])

(def label-atom (reagent/atom "re-frame"))

(defn test-button [label]
	[:button {:on-click #(reset! label "changed...")} @label])

(defn home-page []
	[re-com/v-box
	 :children [[components/nav-bar]
							[re-com/h-box
							 :children [[test-button label-atom]
													[test-button label-atom]
													[test-button label-atom]
													[test-button label-atom]
													[test-dropdown]]]]])

(defn render-app []
	(reagent/render-component [home-page] (.getElementById js/document "app")))

(defn start-app []
	(render-app))
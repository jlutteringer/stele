(ns alloy.stele.frontend.app
	(:require [alloy.stele.frontend.components :as components]
						[alloy.bessemer.core :as b]
						[alloy.anvil.clojure.parse :as parse]
						[alloy.anvil.clojure.util :as util]
						[reagent.core :as reagent]
						[re-frame.core :as re-frame]
						[re-com.core :as re-com]
						[clojure.string :as string])
	(:require-macros [mount.core :refer [defstate]]
									 [reagent.ratom :refer [reaction]]))

(defonce state (reagent/atom "State"))

(defn home-page []
	[b/container
	 [:div.row
		[b/col
		 [[b/example
			 :content [[:div.btn-toolbar
									[b/button @state]
									[b/button "Changed Again" :secondary]
									[b/button "Success" :success]
									[b/button "Info" :info]
									[b/button "Warning" :warning]
									[b/button "Danger" :danger]
									[b/button "Link" :link]]]
			 :code (string/join [";These are buttons and we don't care who knows:\n"
													 "[:button {:class \"btn btn-success\" :type \"button\"} \"Success\"]\n"
													 "[:button {:class \"btn btn-success\" :type \"button\"} \"Success\"]"])]
			[b/example
			 :content [[:div.btn-toolbar
									[b/button "Primary" :outline]
									[b/button "Success" :success :outline]
									[b/button "Info" :info :outline]
									[b/button "Warning" :warning :outline]
									[b/button "Danger" :danger :outline]]]]
			[b/example
			 :content [[:div.btn-toolbar
									[b/button "Primary" :large]
									[b/button "Secondary" :secondary :large]]]]
			[b/example
			 :content [[:div.btn-toolbar
									[b/button "Primary" :small]
									[b/button "Secondary" :secondary :small]]]]
			[b/example
			 :content [[:div.btn-toolbar
									[b/button "Primary" :block]
									[b/button "Secondary" :secondary :block]]]]
			[b/example
			 :content [[:div.btn-toolbar
									[b/button "Primary" :large :disabled]
									[b/button "Secondary" :secondary :large :disabled]]]]]
		 :size 9]
		[b/col "Sidebar"]]])

(defn render-app []
	(reagent/render-component [home-page] (.getElementById js/document "app")))

(defn start-app []
	(render-app))
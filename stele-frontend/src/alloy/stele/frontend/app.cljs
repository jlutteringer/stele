(ns alloy.stele.frontend.app
	(:require [alloy.stele.frontend.components :as components]
						[alloy.bessemer.core :as b]
						[reagent.core :as reagent]
						[re-frame.core :as re-frame]
						[re-com.core :as re-com])
	(:require-macros [mount.core :refer [defstate]]
									 [reagent.ratom :refer [reaction]]))

(defn home-page []
	[b/container
	 [:div.row
		[b/col
		 :size 9
		 :content [[b/button :type :primary :label "Primary"]
							 [:button {:class "btn btn-secondary" :type "button"} "Secondary"]
							 [:button {:class "btn btn-success" :type "button"} "Success"]
							 [:button {:class "btn btn-info" :type "button"} "Info"]
							 [:button {:class "btn btn-warning" :type "button"} "Warning"]
							 [:button {:class "btn btn-danger" :type "button"} "Danger"]
							 [:button {:class "btn btn-link" :type "button"} "Link"]]]
		[b/col :content "Sidebar"]]])

(defn render-app []
	(reagent/render-component [home-page] (.getElementById js/document "app")))

(defn start-app []
	(render-app))
(ns alloy.stele.frontend.app
	(:require [alloy.stele.frontend.components :as components]
						[alloy.bessemer.core :as b]
						[alloy.bessemer.documentation.core :as doc]
						[alloy.bessemer.documentation.components :as doc-components]
						[alloy.anvil.clojure.parse :as parse]
						[alloy.anvil.clojure.util :as util]
						[reagent.core :as reagent]
						[re-frame.core :as re-frame]
						[re-com.core :as re-com]
						[clojure.string :as string])
	(:require-macros [mount.core :refer [defstate]]
									 [reagent.ratom :refer [reaction]]))

(defn home-page []
	[b/container
	 [:div.row
		[b/col
		 [
			[doc-components/section (first (doc/sections doc/global-section-registry))]
			;[:h2 "Contents"]
			;[:ul
			; [:li [b/anchor "Examples" :uri "#examples"]]
			; [:li [b/anchor "Button Tags" :uri "#examples"]]
			; [:li [b/anchor "Outline Buttons" :uri "#examples"]]
			; [:li [b/anchor "Sizes" :uri "#examples"]]
			; [:li [b/anchor "Active State" :uri "#examples"]]
			; [:li [b/anchor "Disables State" :uri "#examples"]]
			; [:li [b/anchor "Button Plugin" :uri "#examples"]
			;	[:ul
			;	 [:li [b/anchor "Toggle States" :uri "#examples"]]
			;	 [:li [b/anchor "Checkbox and Radio Buttons" :uri "#examples"]]
			;	 [:li [b/anchor "Methods" :uri "#examples"]]]]]
			;[:h2 "Examples"]
			;[:p "Bootstrap includes six predefined button styles, each serving its own semantic purpose."]
			;[doc-components/example
			; :content [[:div.btn-toolbar
			;						[b/button "Primary"]
			;						[b/button "Secondary" :secondary]
			;						[b/button "Success" :success]
			;						[b/button "Info" :info]
			;						[b/button "Warning" :warning]
			;						[b/button "Danger" :danger]
			;						[b/button "Link" :link]]]
			; :source (string/join [";These are buttons and we don't care who knows:\n"
			;										 "[:button {:class \"btn btn-success\" :type \"button\"} \"Success\"]\n"
			;										 "[:button {:class \"btn btn-success\" :type \"button\"} \"Success\"]"])]
			;[:div.callout.callout-warning
			; [:h4 "Conveying meaning to assistive technologies"]
			; [:p "Using color to add meaning only provides a visual indication, which will not be conveyed to users of assistive technologies – such as screen readers. Ensure that information denoted by the color is either obvious from the content itself (e.g. the visible text), or is included through alternative means, such as additional text hidden with the .sr-only class."]]
			;[:h2 "Outline Buttons"]
			;[:p "In need of a button, but not the hefty background colors they bring? Replace the default modifier classes with the .btn-outline-* ones to remove all background images and colors on any button."]
			;[doc-components/example
			; :content [[:div.btn-toolbar
			;						[b/button "Primary" :outline]
			;						[b/button "Success" :success :outline]
			;						[b/button "Info" :info :outline]
			;						[b/button "Warning" :warning :outline]
			;						[b/button "Danger" :danger :outline]]]]
			;[doc-components/example
			; :content [[:div.btn-toolbar
			;						[b/button "Primary" :large]
			;						[b/button "Secondary" :secondary :large]]]]
			;[doc-components/example
			; :content [[:div.btn-toolbar
			;						[b/button "Primary" :small]
			;						[b/button "Secondary" :secondary :small]]]]
			;[doc-components/example
			; :content [[:div.btn-toolbar
			;						[b/button "Primary" :block]
			;						[b/button "Secondary" :secondary :block]]]]
			;[doc-components/example
			; :content [[:div.btn-toolbar
			;						[b/button "Primary" :large :disabled]
			;						[b/button "Secondary" :secondary :large :disabled]]]]
			]
		 :size 9]
		[b/col "Sidebar"]]])

(defn render-app []
	(reagent/render-component [home-page] (.getElementById js/document "app")))

(defn start-app []
	(render-app))
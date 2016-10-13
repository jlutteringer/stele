(ns alloy.stele.frontend.app
  (:import goog.History)
  (:require [alloy.stele.frontend.components :as components]
            [alloy.bessemer.core :as b]
            [alloy.bessemer.component.card :as card]
            [alloy.bessemer.documentation.core :as doc]
            [alloy.bessemer.documentation.components :as doc-components]
            [alloy.anvil.clojure.parse :as parse]
            [alloy.anvil.clojure.util :as util]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [clojure.string :as string]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:require-macros [mount.core :refer [defstate]]
                   [reagent.ratom :refer [reaction]]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(def app-state (reagent/atom {:page :home}))

(defn app-routes []
  (secretary/set-config! :prefix "#")

  (secretary/defroute "/" []
            (swap! app-state assoc :page :home))

  (secretary/defroute "/style-guide" []
            (swap! app-state assoc :page :style-guide))

  (hook-browser-navigation!))

(defn home-page []
  [:div "Home Page :)"])

(defn style-guide []
  [doc-components/bessemer-documentation-site
   :sections (doc/sections doc/global-section-registry)])

(defmulti current-page #(@app-state :page))
(defmethod current-page :home []
  [home-page])
(defmethod current-page :style-guide []
  [style-guide])

(defn render-app []
  (app-routes)
  (reagent/render-component [current-page] (.getElementById js/document "app")))

(defn start-app []
  (render-app))
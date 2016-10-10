(ns alloy.stele.frontend.app
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
            [clojure.string :as string])
  (:require-macros [mount.core :refer [defstate]]
                   [reagent.ratom :refer [reaction]]))

(defn home-page []
  [doc-components/bessemer-documentation-site
   :sections (doc/sections doc/global-section-registry)])

(defn render-app []
  (reagent/render-component [home-page] (.getElementById js/document "app")))

(defn start-app []
  (render-app))
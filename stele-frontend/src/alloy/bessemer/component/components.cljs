(ns alloy.bessemer.component.components
  (:require [reagent.core :as reagent]
            [alloy.anvil.clojure.math :as math]
            [resize-observer-polyfill]
            [cljsjs.tether]))

(defn ref [atom content] [:div {:ref (fn [com] (reset! atom com))} content])

(defn click-outside [handler _]
  (let [node (reagent/atom nil)
        handle-click-event #(when-not (.contains @node (.-target %)) (handler %))]
    (reagent/create-class
      {:reagent-render (fn [_ content] [ref node content])
       :component-did-mount #(.addEventListener js/document "click" handle-click-event true)
       :component-will-unmount #(.removeEventListener js/document "click" handle-click-event true)})))

(defn measure [measure _]
  (let [node (reagent/atom nil)
        observer (js/ResizeObserver.
                   (fn [entries _]
                     (let [dimensions (.-contentRect (first entries))]
                       (reset! measure {:width (.-width dimensions) :height (.-height dimensions)}))))]
    (fn [_ contents]
      ;todo is this the best place for calling this?
      (when (some? @node) (.observe observer @node))
      [ref node contents])))

(defn create-tether! [options] (new js/Tether (clj->js options)))

(defn tether []
  (let [target-node (reagent/atom nil)
        target-dimensions (reagent/atom nil)
        element-node (reagent/atom nil)
        tether (reagent/atom nil)]
    (reagent/create-class
      {:reagent-render
       (fn [target element sync-size tether-options]
         (let [target-element [ref target-node target]]
           (when (and (some? @target-node) (some? @element-node))
             (reset! tether (create-tether! (merge {:target @target-node :element @element-node} tether-options))))
           [:div
            (if sync-size [measure target-dimensions target-element] target-element)
            [ref element-node [:div (when sync-size {:style {:width (:width @target-dimensions)}}) element]]]))})))

(defn section-resolver [section-resolver dimensions]
  (let [atom (section-resolver dimensions)]
    (fn [] @atom)))

(defn render-section [options [section-x section-y]]
  (let [content-width (-> options :section-size :width)
        content-height (-> options :section-size :height)]
    ^{:key [section-x section-y]}
    [:div
     {:style {:position "absolute"
              :left (* section-x content-width)
              :top (* section-y content-height)
              :border "1px solid black"
              :width (if (some? content-width) (dec content-width) "100%")
              :height (dec content-height)}}
     [section-resolver (:section-resolver options) [section-x section-y]]]))

(defn calculate-bounds [{:keys [section-matrix section-size]} container-dimensions [scroll-x scroll-y]]
  (let [scroll-points [(/ scroll-x (:width section-size)) (/ scroll-y (:height section-size))]
        bound-1 (map int scroll-points)
        scaling-points [(/ (:width container-dimensions) (:width section-size))
                        (/ (:height container-dimensions) (:height section-size))]
        upper-bound (map dec section-matrix)
        bound-2 (math/point-min (map int (math/point-add scroll-points scaling-points)) upper-bound)]
    [bound-1 bound-2]))

;:width (-> section-size :width (- 1)) :height (-> section-size :height (- 1))
(defn render-sections [options container-dimensions scroll-position]
  (let [[bound-1 bound-2] (calculate-bounds options container-dimensions scroll-position)]
    [:div (map #(render-section options %) (math/points-between bound-1 bound-2))]))

(defn on-scroll [atom]
  #(reset! atom [(-> % .-target .-scrollLeft) (-> % .-target .-scrollTop)]))

(defn scroll-zone [{:keys [container-dimensions]}]
  (let [scroll-position (reagent/atom [0 0])
        container-dimensions (reagent/atom nil)]

    (fn [{:keys [section-matrix section-size height] :as options}]
      (let [content-height (* (-> section-size :height) (-> section-matrix second))
            content-width (* (-> section-size :width) (-> section-matrix first))]
        [:div
         [measure container-dimensions
          [:div.bessemer-scroll-zone {:on-scroll (on-scroll scroll-position) :style {:height height}}
           [:div.scroller-y {:style {:height content-height}}]
           [:div.scroller-x {:style {:width content-width}}]
           (render-sections options @container-dimensions @scroll-position)]]]))))

(def opts
  {:section-matrix [1 100]
   :section-size {:height 75}
   :height 400
   :section-resolver (fn [[section-x section-y]]
                       (let [atom (reagent/atom [:div (str [section-x section-y])])]
                         (js/setTimeout #(reset! atom [:div (str [section-x section-y]) "- Loaded"]) 1000)
                         atom))
   })

(defn dropdown-scaffold [{:keys [open]}]
  (let [content-node (reagent/atom nil)
        deselect-handler #(when-not (.contains @content-node (.-target %)) (reset! open false))]
    (fn [{:keys [label content open]}]
      [click-outside deselect-handler
       [tether
        [:div {:on-click #(swap! open not)} label]
        [ref content-node (if @open content [:div])]
        true
        {:attachment "top left"
         :targetAttachment "bottom left"
         :constraints [{:to "window"
                        :attachment "together"}]}]])))

(defn dropdown [{:keys [open]}]
  (let [content-node (reagent/atom nil)
        deselect-handler #(when-not (.contains @content-node (.-target %)) (reset! open false))]
    (fn [{:keys [label content open]}]
      [click-outside deselect-handler
       [tether
        [:div {:on-click #(swap! open not)} label]
        [ref content-node (if @open content [:div])]]])))

(defn dropdown-list [])

(defn input [{:keys []}]
  [tether
   [:div.bessemer-input
    [:div.input-zone
     [:input {:placeholder "Select..."}]]
    [:div.control-zone [:i.fa.fa-times]]
    [:div.control-zone [:i.fa.fa-caret-down]]]
   [:div {:style {:background-color "white"}} [scroll-zone opts]]])

(defn blah []
  (let []
    (fn []
      [:div
       [dropdown-scaffold
        {:label [:div.bessemer-input
                 [:div.input-zone
                  [:input {:placeholder "Select..."}]]
                 [:div.control-zone [:i.fa.fa-times]]
                 [:div.control-zone [:i.fa.fa-caret-down]]]
         :content [:div {:style {:background-color "white"}} [scroll-zone opts]]
         :sync-content-size true
         :open (reagent/atom false)}]

       [dropdown-scaffold
        {:label [:div.bessemer-input
                 [:div.input-zone
                  [:input {:placeholder "Select..."}]]
                 [:div.control-zone [:i.fa.fa-times]]
                 [:div.control-zone [:i.fa.fa-caret-down]]]
         :content [:div {:style {:background-color "white"}}
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]]
         :open (reagent/atom false)}]])))

(ns alloy.bessemer.component.components
  (:require [reagent.core :as reagent]
            [alloy.anvil.clojure.util :as util]
            [alloy.anvil.clojure.math :as math]
            [resize-observer-polyfill]
            [cljsjs.tether]))

;todo default arguments (handled through draft)
;todo there are alot of repeated events eg... on-click, which are passed manually down the component heirarchies... this should be able to be less manual
;todo move "utility" components to a seperate namespace
;todo the style attribues on non-component components need some work... can we accomplish this without an extra div stuck in there?

(defn primitive-component? [element] (keyword? (first element)))

;todo this implementation is incomplete
(defn merge-attribute-maps [current-attributes additional-attributes]
  (merge current-attributes additional-attributes))

(defn attribute-map? [attributes] (map? attributes))

(defn merge-primitive-attributes [[type potential-attributes & remaining :as element] attributes]
  (if (attribute-map? potential-attributes)
    [type (merge-attribute-maps potential-attributes attributes) (when (util/not-empty? remaining) remaining)]
    [(first element) attributes (when (util/not-empty? (rest element)) (rest element))]))

(defn merge-attributes [element attributes]
  (if (primitive-component? element)
    (merge-primitive-attributes element attributes)
    [:div (merge-attribute-maps attributes {:style {:display "inline-flex" :width "100%"}}) element]))

;todo for some reason we can't call this like (ref ...) and have to do [ref ...] otherwise we get an infinite render loop, we need to figure that out
(defn ref [atom content]
  (merge-attributes content {:ref (fn [com] (reset! atom com))}))

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
                       (reset! measure {:width (int (.-width dimensions)) :height (int (.-height dimensions))}))))]
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
       (fn [{:keys [target element sync-size tether-options]}]
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

(defn scroll-zone []
  (let [scroll-position (reagent/atom [0 0])
        container-size (reagent/atom nil)]

    (fn [{:keys [section-matrix section-size height] :as options}]
      (let [content-height (* (-> section-size :height) (-> section-matrix second))
            content-width (* (-> section-size :width) (-> section-matrix first))]
        [:div
         [measure container-size
          [:div.bessemer-scroll-zone {:on-scroll (on-scroll scroll-position) :style {:height height :width "100%"}}
           [:div.scroller-y {:style {:height content-height}}]
           [:div.scroller-x {:style {:width content-width}}]
           (render-sections options @container-size @scroll-position)]]]))))

(def opts
  {:section-matrix [1 100000]
   :section-size {:height 75}
   :height 400
   :section-resolver (fn [[section-x section-y]]
                       (let [atom (reagent/atom [:div (str [section-x section-y])])]
                         (js/setTimeout #(reset! atom [:div (str [section-x section-y]) "- Loaded"]) 1000)
                         atom))
   })

(defn control [{:keys [content attributes]}]
  [:div.bessemer-input attributes
   (map #(if (and (vector? %) (= (first %) :control))
           [:div.control-zone (second %)]
           [:div.input-zone %]) (filter some? content))])

(defn focus! [element] (.focus element))
(defn focused? [element] (= (.-activeElement js/document) element))

(defn event-get-target [event] (.-target event))
(defn event-get-value [event] (-> event event-get-target .-value))

(defn input []
  (let [input-ref (reagent/atom nil)
        value (reagent/atom "")]
    (fn [{:keys [placeholder clearable edit-mode]}]
      [control
       {:content [[:span
                   [:input
                    {:on-change #(reset! value (event-get-value %))
                     :value @value}]]
                  (when (and clearable (util/not-empty? @value))
                    [:control [:i.fa.fa-times {:on-click #(reset! value "")}]])]}])))

(defn input-edit []
  (let [edit-state (reagent/atom :inactive)
        input-ref (reagent/atom nil)
        value (reagent/atom "")
        input-handler #(do (reset! value (event-get-value %))
                           (when (= @edit-state :edit-start) (reset! edit-state :editing)))]
    (fn [{:keys [placeholder clearable edit-mode]}]
      (when (some? @input-ref) (focus! @input-ref))

      [control
       {:content [[:span {:style {:width "100%"}}
                   (cond
                     (= :inactive @edit-state)
                     [:span @value]

                     (= :edit-start @edit-state)
                     [:span
                      [ref input-ref [:input
                                      {:style {:width 1}
                                       :on-change input-handler}]]
                      @value]
                     (= :editing @edit-state)
                     [:span
                      [ref input-ref [:input
                                      {:style {:width "100%"}
                                       :on-change input-handler
                                       :value @value}]]]
                     )]
                  (when (and clearable (util/not-empty? @value))
                    [:control [:i.fa.fa-times {:on-click #(reset! value "")}]])]
        :attributes {:on-click #(when (= @edit-state :inactive) (reset! edit-state :edit-start))
                     :on-blur  #(reset! edit-state :inactive)}}])))

;todo move "dropdown" components to a seperate namespace
(def dropdown-tether-options {:attachment "top left"
                              :targetAttachment "bottom left"
                              :constraints [{:to "window"
                                             :attachment "together"}]})
(defn dropdown-scaffold [{:keys [open]}]
  (let [content-node (reagent/atom nil)
        deselect-handler #(when-not (.contains @content-node (.-target %)) (reset! open false))]
    (fn [{:keys [label content open sync-content-size on-click]}]
      [click-outside deselect-handler
       [tether
        {:target [:div {:on-click #(do
                                     (swap! open not)
                                     (if (fn? on-click) (on-click %)))} label]
         :element [ref content-node (if @open content [:div])]
         :sync-size sync-content-size
         :tether-options dropdown-tether-options}]])))

;todo, when sync-content-size is true we dont need to wrap in measure components to get borders to work
;todo some of the sizing code here could likely be refactored
(defn dropdown []
  (let [label-size (reagent/atom nil)
        content-size (reagent/atom nil)]
    (fn [{:keys [label content open sync-content-size on-click]}]
      [dropdown-scaffold
       {:label [measure label-size
                [:div.bessemer-input
                 (when @open {:class (str "dropdown-open "
                                          (when (and (some? @label-size) (some? @content-size))
                                            (cond (> (@label-size :width) (@content-size :width)) "bordered"
                                                  (< (@label-size :width) (@content-size :width)) "borderless"
                                                  :else "")))})
                 [:div.input-zone label]
                 [:div.control-zone (if @open [:i.fa.fa-caret-up] [:i.fa.fa-caret-down])]]]
        :content [measure content-size
                  [:div.dropdown-zone (when (and (some? @label-size) (some? @content-size)
                                                 (< (@label-size :width) (@content-size :width)))
                                        {:class "bordered"}) content]]
        :sync-content-size sync-content-size
        :open open
        :on-click on-click}])))

(defn dropdown-list [{:keys [label items open sync-content-size]}]
  [dropdown
   {:label label
    :content [:div (map #(-> [:span.dropdown-item %]) items)]
    :open open
    :sync-content-size sync-content-size}])

(defn create-select-input [{:keys [placeholder multi-select selected-values input-ref]}]
  [:div
   [:span (if (empty? @selected-values)
            placeholder
            (-> @selected-values first :label))]
   [ref input-ref [:input]]])

(defn create-select-handler [element state]
  #(do
     (if (:multi-select state)
       [] ;todo multi select support
       (reset! (:selected-values state) [element]))
     (reset! (:open state) false)))

;todo select and dropdown list components share some structure... opportunity for reuse?
(defn select []
  (let [input-ref (reagent/atom nil)
        dropdown-ref (reagent/atom nil)
        selected-values (reagent/atom [])]
    (fn [{:keys [placeholder items open sync-content-size multi-select] :as options}]
      ;todo draft should be able to handle this boilerplate... start looking into a defcomponent macro sooner rather than later
      (let [state (merge options {:selected-values selected-values :input-ref input-ref})]
        [ref dropdown-ref
         [dropdown
          {:label (create-select-input state)
           :content [:div (map #(-> [:span.dropdown-item.clickable
                                     {:on-click (create-select-handler % state)}
                                     (:label %)]) items)]
           :open open
           :sync-content-size sync-content-size
           :on-click #(if @open (focus! @input-ref) (focus! @dropdown-ref))}]]))))

(defn blah []
  (let []
    (fn []
      [:div {:style {:height 4000}}
       [control
        {:content ["Label 1"
                   [:control [:i.fa.fa-times]]
                   "Label 2"
                   [:control [:i.fa.fa-times]]
                   "Control"
                   [:control [:i.fa.fa-times]]
                   [:control [:i.fa.fa-times]]]}]

       [:hr]

       [input
        {:placeholder "Standard Edit"
         :clearable true
         :edit-mode :standard}]

       [:hr]

       [input-edit
        {:placeholder "Replace Edit"
         :clearable true
         :edit-mode :replace}]

       [:hr]

       [dropdown-scaffold
        {:label [:div.bessemer-input
                 [:div.input-zone
                  [:input {:placeholder "Select..."}]]
                 [:div.control-zone [:i.fa.fa-times]]
                 [:div.control-zone [:i.fa.fa-caret-down]]]
         :content [:div {:style {:background-color "white" :width "100%"}} [scroll-zone opts]]
         :sync-content-size true
         :open (reagent/atom false)}]

       [:hr]

       [dropdown
        {:label "Big Label, Small Content"
         :content [:div
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]]
         :sync-content-size false
         :open (reagent/atom false)}]

       [:hr]

       [dropdown
        {:label "Small Label, Big Content"
         :content [:div {:style {:width 500}}
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]]
         :sync-content-size false
         :open (reagent/atom false)}]

       [:hr]

       [dropdown
        {:label "Same Size"
         :content [:div
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]
                   [:div "Blah"]]
         :sync-content-size true
         :open (reagent/atom false)}]

       [:hr]

       [dropdown-list
        {:label "Dropdown List"
         :items ["Blah"
                 "Blah"
                 "Blah"
                 "Blah"
                 "Blah"
                 "Blah"
                 "Blah"]
         :sync-content-size true
         :open (reagent/atom false)}]

       [:hr]

       [:nav.navbar.navbar-toggleable-md.navbar-light.bg-faded
        [:a.navbar-brand {:href "#"} "Navbar"]
        [:ul.navbar-nav
         [:li.nav-item.active [:a.nav-link "Home"]]
         [:li.nav-item [:a.nav-link "Features"]]
         [:li.nav-item [:a.nav-link "Pricing"]]
         [:li.nav-item
          [dropdown-list
           {:label "Dropdown List"
            :items ["Blah"
                    "Blah"
                    "Blah"
                    "Blah"
                    "Blah"
                    "Blah"
                    "Blah"]
            :sync-content-size true
            :open (reagent/atom false)}]]]]

       [:hr]

       [select
        {:placeholder "Select"
         :items [{:label "Blah 1" :value 1}
                 {:label "Blah 2" :value 2}
                 {:label "Blah 3" :value 3}
                 {:label "Blah 4" :value 4}
                 {:label "Blah 5" :value 5}
                 {:label "Blah 6" :value 6}]
         :sync-content-size true
         :multi-select false
         :open (reagent/atom false)}]

       ])))
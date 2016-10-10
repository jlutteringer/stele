(ns alloy.bessemer.core
  (:require
    [reagent.core :as reagent]
    [alloy.anvil.clojure.util :as util :include-macros true]
    [alloy.anvil.clojure.schema :as schema]
    [alloy.bessemer.documentation.core :as doc]
    [clojure.string :as string]
    [alloy.bessemer.util :as butil]
    [clojure.string :as str]))

(doc/def-section ::components
                 :title "Components"
                 :description "Over a dozen reusable components built to provide buttons, dropdowns, input groups, navigation, alerts, and much more.")

(defn container [& args]
  (util/concat-vec :div.container args))

(def col-schema (butil/web-element [::col
                                    :fields [[:size]
                                             [:content :primary]]]))
(def col
  (butil/component
    (fn [{:keys [size content]}]
      (util/concat-vec
        [:div
         {:class (str "col-xs" (when (some? size) (str "-" size)))}] content))
    :schema col-schema
    :static))

(def image-schema (butil/web-element [::image
                                      :fields [[:uri :primary]]]))
(def image
  (butil/component
    (fn [{:keys [uri] :as args}]
      [:img (butil/make-attributes args {:src uri})])
    :schema image-schema
    :static))

(def anchor-schema (butil/web-element [::anchor
                                       :name "Anchors"
                                       :fields [[:label :primary]
                                                [:uri :default "#"]]]))
(doc/def-schema-section anchor-schema
                        :description "Use Bootstrap’s custom button styles for actions in forms, dialogs, and more. Includes support for a handful of contextual variations, sizes, states, and more.")
(def anchor
  (butil/component
    (fn [{:keys [label uri] :as args}]
      [:a (butil/make-attributes args {:class "" :href uri}) label])
    :schema anchor-schema
    :static))

(def callout-types #{:primary :info :warning :danger})
(def callout-schema (butil/web-element [::callout
                                        :name "Callouts"
                                        :fields [[:header :primary]
                                                 [:body]
                                                 [:type :default :primary :layout-type [:enumerated callout-types]]]]))
(def callout
  (butil/component
    (fn [{:keys [header body type] :as args}]
      [:div.callout {:class (str "callout-" (name type))}
       [:h4 header]
       [:p body]])
    :schema callout-schema
    :static))

(def button-types #{:primary :secondary :success :info :warning :danger :link})
(def button-sizes #{:default :large :small :block})

(def button-schema (butil/web-element [::button
                                       :name "Buttons"
                                       :fields [[:label :primary]
                                                [:type :default :primary :layout-type [:enumerated button-types]]
                                                [:outline :default false :layout-type :flag]
                                                [:size :default :default :layout-type [:enumerated button-sizes]]
                                                [:disabled :default false :layout-type :flag]
                                                [:click :default util/empty-fn]]]))

(doc/def-schema-section button-schema
                        :description "Use Bootstrap’s custom button styles for actions in forms, dialogs, and more. Includes support for a handful of contextual variations, sizes, states, and more.")
(def button
  (butil/component
    (fn [{:keys [label type outline size disabled click] :as args}]
      (if (string? label)
        [:button (butil/make-attributes
                   args
                   {:class    ["btn"
                               (str "btn-" (when outline "outline-") (name type))
                               (cond
                                 (= :default size) ""
                                 (= :large size) "btn-lg"
                                 (= :small size) "btm-sm"
                                 (= :block size) "btn-block")]
                    :on-click (butil/handler-fn click)}
                   (when disabled
                     {:disabled "disabled"})) label]
        (butil/make-merged-element label anchor-schema args {:class ["btn" (str "btn-" (name type))]})))
    :schema button-schema
    :static))

(doc/def-example
  ::examples
  :title "Examples"
  :description "Bootstrap includes six predefined button styles, each serving its own semantic purpose."
  :additional-content [callout "Conveying meaning to assistive technologies"
                       :body "Using color to add meaning only provides a visual indication, which will not be conveyed to users of assistive technologies – such as screen readers. Ensure that information denoted by the color is either obvious from the content itself (e.g. the visible text), or is included through alternative means, such as additional text hidden with the .sr-only class."
                       :warning])

(doc/add-example [[:div.btn-toolbar
                   [button "Primary"]
                   [button "Secondary" :secondary]
                   [button "Success" :success]
                   [button "Info" :info]
                   [button "Warning" :warning]
                   [button "Danger" :danger]
                   [button "Link" :link]]])

(doc/def-example
  ::outline-buttons
  :title "Outline Buttons"
  :description "In need of a button, but not the hefty background colors they bring? Replace the default modifier classes with the .btn-outline-* ones to remove all background images and colors on any button."
  )

(doc/add-example [[:div.btn-toolbar
                   [button "Primary" :outline]
                   [button "Secondary" :secondary :outline]
                   [button "Success" :success :outline]
                   [button "Info" :info :outline]
                   [button "Warning" :warning :outline]
                   [button "Danger" :danger :outline]]])

(doc/def-example
  ::sizes
  :title "Sizes"
  :description "Fancy larger or smaller buttons? Add .btn-lg or .btn-sm for additional sizes.")

(doc/add-example [[:div.btn-toolbar
                   [button "Large Button" :large]
                   [button "Large Button" :secondary :large]]])

(doc/add-example [[:div.btn-toolbar
                   [button "Small Button" :small]
                   [button "Small Button" :secondary :small]]])

(doc/add-example [[:div.btn-toolbar
                   [button "Block Level Button" :block]
                   [button "Block Level Button" :secondary :block]]]
                 :description "Create block level buttons—those that span the full width of a parent—by adding .btn-block.")

(doc/def-example ::disabled-state
                 :title "Disabled State"
                 :description "Make buttons look inactive by adding the disabled boolea attribute to and button element")

(doc/add-example [[:div.btn-toolbar
                   [button "Primary Button" :large :disabled]
                   [button "Button" :secondary :large :disabled]]])

(doc/add-example [[:div.btn-toolbar
                   [button [anchor "Primary Link" :uri "#"] :large :disabled]
                   [button [anchor "Link" :uri "#"] :secondary :large :disabled]]])

(def alert-types #{:success :info :warning :danger})
(def alert-schema (butil/web-element [::alert
                                      :name "Alerts"
                                      :fields [[:header]
                                               [:body]
                                               [:type :default :success :layout-type [:enumerated alert-types]]
                                               [:dismiss :default-fn (util/ignore-args #(reagent/atom false))]]]))
(doc/def-schema-section alert-schema
                        :description "Use Bootstrap’s custom button styles for actions in forms, dialogs, and more. Includes support for a handful of contextual variations, sizes, states, and more.")
(def alert
  (butil/component
    (fn [{:keys [header body type dismiss] :as args}]
      (when (or (= dismiss :disable) (not @dismiss))
        [:div.alert {:class (str "alert-" (name type))}
         (when (not (= dismiss :disable)) [:button {:class "close" :on-click #(reset! dismiss true)} "×"])
         (when (some? header) [:h4.alert-heading header])
         body]))
    :schema alert-schema))

(doc/def-example ::examples
                 :title "Examples"
                 :description "Alerts are available for any length of text, as well as an optional dismiss button. For proper styling, use one of the four required contextual classes (e.g., .alert-success). For inline dismissal, use the alerts jQuery plugin."
                 :additional-content [callout "Conveying meaning to assistive technologies"
                                      :body "Using color to add meaning only provides a visual indication, which will not be conveyed to users of assistive technologies – such as screen readers. Ensure that information denoted by the color is either obvious from the content itself (e.g. the visible text), or is included through alternative means, such as additional text hidden with the .sr-only class."
                                      :warning])

(doc/add-example [[:div
                   [alert :body "Well done! You successfully read this important alert message." :dismiss :disable]
                   [alert :body "Heads up! This alert needs your attention, but it's not super important." :info :dismiss :disable]
                   [alert :body "Warning! Better check yourself, you're not looking too good." :warning :dismiss :disable]
                   [alert :body "Oh snap! Change a few things up and try submitting again." :danger :dismiss :disable]]])

(doc/def-example ::additional-content
                 :title "Additional Content"
                 :description "Alerts can also contain additional HTML elements like headings and paragraphs.")

(doc/add-example [[:div
                   [alert
                    :header "Well Done!"
                    :body [[:p "Aww yeah, you successfully read this important alert message. This example text is going to run a bit longer so that you can see how spacing within an alert works with this kind of content."]
                           [:p "Whenever you need to, be sure to use margin utilities to keep things nice and tidy."]]
                    :dismiss :disable]]])

(doc/def-example ::dismissing
                 :title "Dismissing"
                 :description [[:p "Using the alert JavaScript plugin, it’s possible to dismiss any alert inline. Here’s how:"]
                               [:ul
                                [:li "Be sure you’ve loaded the alert plugin, or the compiled Bootstrap JavaScript."]
                                [:li "Add a dismiss button and the .alert-dismissible class, which adds extra padding to the right of the alert and positions the .close button."]
                                [:li "On the dismiss button, add the data-dismiss=\"alert\" attribute, which triggers the JavaScript functionality. Be sure to use the <button> element with it for proper behavior across all devices."]]])

(doc/add-example [[:div
                   [alert :body "Well done! You successfully read this important alert message."]
                   [alert :body "Heads up! This alert needs your attention, but it's not super important." :info]
                   [alert :body "Warning! Better check yourself, you're not looking too good." :warning]
                   [alert :body "Oh snap! Change a few things up and try submitting again." :danger]]])


;TODO evaluate the data types used for breadcrumbs... pairs might not make the best sense, some combination with anchors?
(def breadcrumb-schema (butil/web-element [::breadcrumb
                                           :name "Breadcrumb"
                                           :fields [[:breadcrumbs :primary]]]))
(doc/def-schema-section breadcrumb-schema
                        :description "Indicate the current page’s location within a navigational hierarchy. Separators are automatically added in CSS through ::before and content.")
(def breadcrumb
  (butil/component
    (fn [{:keys [breadcrumbs]}]
      [:ol.breadcrumb
       (map-indexed (fn [i val]
                      (let [active (= i (dec (count breadcrumbs)))]
                        [:li.breadcrumb-item
                         (when active {:class "active"})
                         (if (util/pair? val)
                           (if active
                             (first val)
                             [anchor (first val) :uri (second val)])
                           val)])) breadcrumbs)])
    :schema breadcrumb-schema
    :static))

(doc/def-example ::examples :title "Examples")

(doc/add-example [[:div
                   [breadcrumb :breadcrumbs [["Home" "#"]]]
                   [breadcrumb :breadcrumbs [["Home" "#"] ["Home" "#"]]]
                   [breadcrumb :breadcrumbs ["Home" "Home" ["Home" "#"]]]]])


(def overlay-schema (butil/web-element [::overlay
                                        :name "Overlay"
                                        :fields [[:body :primary]
                                                 [:display :default false]]]))
(doc/def-schema-section overlay-schema
                        :description "Desc")

(def overlay
  (butil/component
    (fn [{:keys [body display] :as args}]
      (println display)
      [:div.overlay-container
       body
       (when display [:div.modal-backdrop.fade.in])])
    :schema overlay-schema
    :static))

(doc/def-example ::additional-content
                 :title "Additional Content"
                 :description "Alerts can also contain additional HTML elements like headings and paragraphs.")

(doc/add-example :example [(let [toggle (reagent/atom false)]
                             (fn [] [:div
                                     [button "Toggle Overlay" :large :click #(swap! toggle not)]
                                     [:br] [:br]
                                     [overlay [[:div.card.card-block
                                                [:p "This is an example with some content"]
                                                [:p "More content!"]
                                                [button "A Button" :large]]]
                                      :display @toggle]]))])

(def portal-schema (butil/web-element [::portal
                                       :name "Portal"
                                       :fields [[:body :primary]
                                                [:display :default false]]]))
(def portal
  (butil/component
    (fn [{:keys [body display] :as args}]
      (when display
        [:div.modal.fade.in {:style {:display "block"}}
         [:div.modal-dialog
          [:div.modal-content
           body]]]))
    :schema portal-schema
    :static))

(doc/add-example :example [(let [toggle (reagent/atom false)]
                             (fn [] [:div
                                     [button "Toggle Portal" :large :click #(swap! toggle not)]
                                     [:br] [:br]
                                     [overlay [[:div.card.card-block
                                                [portal
                                                 [[:div.modal-body
                                                   [:p "A portal!"]
                                                   [button "Another Button" :large]]] :display @toggle]
                                                [:p "This is an example with some content"]
                                                [:p "More content!"]
                                                [button "A Button" :large]]]
                                      :display @toggle]]))])

(def modal-schema (butil/web-element [::modal
                                      :name "Modal"
                                      :fields [[:body :primary]
                                               [:display :default false]]]))
(def modal
  (butil/component
    (fn [{:keys [body display] :as args}]
      [portal [
               [:div.modal-body body]]
       :display display])
    :schema modal-schema
    :static))

(doc/add-example :example [(let [toggle (reagent/atom false)]
                             (fn [] [:div
                                     [button "Toggle Modal" :large :click #(swap! toggle not)]
                                     [:br] [:br]
                                     [overlay [[:div.card.card-block
                                                [modal
                                                 [[:p "A portal!"]
                                                  [button "Another Button" :large]] :display @toggle]
                                                [:p "This is an example with some content"]
                                                [:p "More content!"]
                                                [button "A Button" :large]]]
                                      :display @toggle]]))])
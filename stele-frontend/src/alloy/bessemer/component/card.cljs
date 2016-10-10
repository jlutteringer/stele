(ns alloy.bessemer.component.card
	(:require [alloy.bessemer.core :as b]
						[reagent.core :as reagent]
						[alloy.anvil.clojure.util :as util :include-macros true]
						[alloy.anvil.clojure.schema :as schema]
						[alloy.bessemer.documentation.core :as doc]
						[clojure.string :as string]
						[alloy.bessemer.util :as butil]
						[clojure.string :as str]))

(doc/use-section ::b/components)

(def card-block-schema (butil/web-element [::card
																					 :name "Card"
																					 :fields [[:body :primary]]]))

(def block
	(butil/component
		(fn [{:keys [body] :as args}]
			)
		:schema card-block-schema
		:static))

(def card-schema (butil/web-element [::card
																		 :name "Card"
																		 :fields [[:body :primary]]]))

(def card
	(butil/component
		(fn [{:keys [body] :as args}]
			[:div
			 (map (fn [e] [:div "Hello!"]) (util/pour body))])
		:schema card-schema
		:static))

(doc/def-schema-section card-schema :description "Desc")

(doc/def-example ::examples
								 :title "Examples"
								 :description "Cards require a small amount of markup and classes to provide you with as much control as possible.")

(doc/add-example :example [[card
														[[b/image "data:image/svg+xml;charset=UTF-8,<svg%20width%3D\"318\"%20height%3D\"180\"%20xmlns%3D\"http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg\"%20viewBox%3D\"0%200%20318%20180\"%20preserveAspectRatio%3D\"none\"><defs><style%20type%3D\"text%2Fcss\">%23holder_157afc69c24%20text%20%7B%20fill%3Argba(255%2C255%2C255%2C.75)%3Bfont-weight%3Anormal%3Bfont-family%3AHelvetica%2C%20monospace%3Bfont-size%3A16pt%20%7D%20<%2Fstyle><%2Fdefs><g%20id%3D\"holder_157afc69c24\"><rect%20width%3D\"318\"%20height%3D\"180\"%20fill%3D\"%23777\"><%2Frect><g><text%20x%3D\"118.0859375\"%20y%3D\"97.35\">318x180<%2Ftext><%2Fg><%2Fg><%2Fsvg>"]
														 [block
															:title "Card Title"
															[[:p "Some quick example text to build on the card title and make up the bulk of the card's content."]
															 [b/button [b/anchor "Go Somewhere"]]]]]]])

(doc/add-example :example [[:div.card
														[:img {:class "card-img-top" :src "data:image/svg+xml;charset=UTF-8,<svg%20width%3D\"318\"%20height%3D\"180\"%20xmlns%3D\"http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg\"%20viewBox%3D\"0%200%20318%20180\"%20preserveAspectRatio%3D\"none\"><defs><style%20type%3D\"text%2Fcss\">%23holder_157afc69c24%20text%20%7B%20fill%3Argba(255%2C255%2C255%2C.75)%3Bfont-weight%3Anormal%3Bfont-family%3AHelvetica%2C%20monospace%3Bfont-size%3A16pt%20%7D%20<%2Fstyle><%2Fdefs><g%20id%3D\"holder_157afc69c24\"><rect%20width%3D\"318\"%20height%3D\"180\"%20fill%3D\"%23777\"><%2Frect><g><text%20x%3D\"118.0859375\"%20y%3D\"97.35\">318x180<%2Ftext><%2Fg><%2Fg><%2Fsvg>"}]
														[:div.card-block
														 [:h4.card-title "Card Title"]
														 [:p.card-text "Some quick example text to build on the card title and make up the bulk of the card's content."]]]])
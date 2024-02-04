(ns storm.stormdnd.engine.entity
  (:require [storm.stormdnd.engine.engine :as engine]
            [storm.stormdnd.web.routes.block :as b]))

(defn damage
  ([n]
   (engine/with-entity damage n))
  ([entity n]
   (update entity :hp - n)))

(defn set-attribute
  ([attribute-name nv]
   (engine/with-entity set-attribute attribute-name nv))
  ([entity attribute-name nv]
   (assoc entity (keyword attribute-name) nv)))

(defn make-entity [{:keys [stats] :as block} name type hp initiative]
  {:name       name
   :type       type
   :hp         hp
   :ac         (:ac stats)
   :initiative initiative
   :block      block})

(defn add-entity
  ([id name type]
   (engine/with-world add-entity id name type))
  ([world id name type]
   (let [block (b/get-block type)
         entity (-> block
                    (make-entity name type 0 0)                  ; todo roll dices
                    (with-meta {:type :entity}))]
     (if block
       (engine/add-to-world world id entity)
       world))))

(defn make-set-input [id attribute-name label placeholder-v]
  [:form {:hx-put (str "/set/" attribute-name "/" (clojure.core/name id)) :hx-target "#main" :hx-swap "innerHTML"}
   [:div
    [:label.encounter-property-line label]
    [:input {:type "number" :name "nv" :placeholder placeholder-v :autocomplete "off"}]]])

(defn entity-view [[id {:keys [name type hp ac initiative]}]]
  [:div.creature-heading
   [:section.container
    [:div.part
     [:h1
      [:form {:hx-put (str "/change-name/" (clojure.core/name id)) :hx-target "#main" :hx-swap "innerHTML"}
       [:input {:type "text" :name "nv" :placeholder name :autocomplete "off"}]]]
     [:form {:hx-put (str "/damage/" (clojure.core/name id)) :hx-target "#main" :hx-swap "innerHTML"}
      [:div
       [:label.encounter-property-line "HP:"]
       [:input {:type "number" :name "hp" :placeholder hp :autocomplete "off"}]]]
     (make-set-input id "ac" "AC:" ac)
     (make-set-input id "initiative" "Initiative:" initiative)]
    [:div.part-small
     [:button {:hx-put (str "/show-block/" type) :hx-target "#details" :hx-swap "innerHTML"} "show"]
     [:button {:hx-post (str "/remove-entity/" (clojure.core/name id)) :hx-target "#main" :hx-swap "innerHTML"} "remove"]]]
   [:svg.tapered-rule {:height "5" :width "100%"}
    [:polyline {:points "0,0 400,2.5 0,5"}]]])

(defn encounter-view [entities]
  [:div
   [:div.stat-block.encounter
    [:hr.orange-border]
    (map entity-view entities)]])

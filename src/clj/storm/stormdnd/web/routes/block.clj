(ns storm.stormdnd.web.routes.block
  (:require [clojure.string :as str]
            [storm.stormdnd.engine.dice :as dice]
            [cheshire.core :as c]))

(defn add-block [db b]
  (let [block (with-meta b {:type :block})]
    (conj db block)))

(defn upgrade-format [b]
  (let [name (:name b)
        ability-scores (->> (:abilityScores b)
                            (reduce #(conj %1 {(keyword (:abilityType %2)) (:score %2)}) {}))
        stats (->> (:stats b)
                   (reduce #(conj %1 {(keyword (:statType %2)) (:statValue %2)}) {}))
        features (:features b)
        actions (map #(select-keys % [:name :description]) (:actions b))]
    {:name           name
     :ability-scores ability-scores
     :stats          stats
     :features       features
     :actions        actions}))

(def block-db
  (let [monsters (c/parse-string (slurp "resources/public/all.json") true)
        monster-files (map #(str % ".json") monsters)
        blocks (map #(c/parse-string (slurp (str "resources/public/db/" %))
                                     true) monster-files)
        upgraded-blocks (map upgrade-format blocks)]
    (reduce add-block [] upgraded-blocks)))

(defn is-same-block-type [type block]
  (= (str/lower-case type) (str/lower-case (:name block))))

(defn get-block [type]
  (some #(when (is-same-block-type type %) %) block-db))

(defn calculate-modifier [score]
  (-> score (- 10) (/ 2) Math/floor int))

(defn block-view [{:keys [name ability-scores stats features actions]}]
  [:div.stat-block.wide
   [:hr.orange-border]
   [:div.section-left
    [:div.creature-heading
     [:h1 name]]
    [:svg.tapered-rule {:height "5" :width "100%"}
     [:polyline {:points "0,0 400,2.5 0,5"}]]
    [:div.top-stats
     [:div.property-line.first
      [:h4 "Armor Class "]
      [:p (:ac stats)]]
     [:div.property-line
      [:h4 "Hit Points "]
      [:p (dice/average-dice-value (:hp stats)) " (" (:hp stats) ")"]]
     [:div.property-line.last
      [:h4 "Speed "]
      [:p (:speed stats)]]
     [:svg.tapered-rule {:height "5" :width "100%"}
      [:polyline {:points "0,0 400,2.5 0,5"}]]
     [:div.abilities
      [:div.ability-strength
       [:h4 "STR"]
       [:p (:str ability-scores) " (" (calculate-modifier (:str ability-scores)) ")"]]
      [:div.ability-dexterity
       [:h4 "DEX"]
       [:p (:dex ability-scores) " (" (calculate-modifier (:dex ability-scores)) ")"]]
      [:div.ability-constitution
       [:h4 "CON"]
       [:p (:con ability-scores) " (" (calculate-modifier (:con ability-scores)) ")"]]
      [:div.ability-intelligence
       [:h4 "INT"]
       [:p (:int ability-scores) " (" (calculate-modifier (:int ability-scores)) ")"]]
      [:div.ability-wisdom
       [:h4 "WIS"]
       [:p (:wis ability-scores) " (" (calculate-modifier (:wis ability-scores)) ")"]]
      [:div.ability-charisma
       [:h4 "CHA"]
       [:p (:cha ability-scores) " (" (calculate-modifier (:cha ability-scores)) ")"]]]]
    [:svg.tapered-rule {:height "5" :width "100%"}
     [:polyline {:points "0,0 400,2.5 0,5"}]]
    (let [feature-views (mapv #(vector :div.property-block
                                       [:h4 (:name %)]
                                       [:p " " (:description %)])
                              features)]
      (into [:span] feature-views))]
   [:div.section-right
    [:div.actions
     [:h3 "Actions"]
     [:div.actions
      (let [actions-views (mapv #(vector :div.property-block
                                         [:h4 (:name %)]
                                         [:p " " (:description %)])
                                actions)]
        (into [:span] actions-views))
      ]]]
   [:hr.orange-border.bottom]])
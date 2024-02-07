(ns storm.stormdnd.web.routes.ui
  (:require
    [storm.stormdnd.engine.entity :as entity]
    [storm.stormdnd.web.routes.block :as b]
    [storm.stormdnd.engine.engine :as engine]
    [storm.stormdnd.web.middleware.exception :as exception]
    [storm.stormdnd.web.middleware.formats :as formats]
    [hiccup.util :as h-utils]
    [storm.stormdnd.web.routes.utils :as utils]
    [storm.stormdnd.web.htmx :refer [ui page] :as htmx]
    [integrant.core :as ig]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]))

; TODO replace by uid ?
(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(def mock-history
  (-> nil
      (engine/add-to-history `(entity/add-entity :Astarion "Astarion" "goblin"))
      (engine/add-to-history `(engine/update-entity :Astarion (entity/damage 1)))
      (engine/add-to-history `(entity/add-entity :Baldur "Baldur" "goblin"))))

(defn search-block-input-view [search]
  [:form {:hx-get (str "/show-add-view") :hx-target "#details" :hx-swap "innerHTML"}
   [:input {:type "text" :name "search" :placeholder search :autocomplete "on"}]
   [:button "Search"]])

(defn add-entity-button-view [entity-type]
  [:form {:hx-post "/add" :hx-target "#main" :hx-swap "innerHTML"}
   [:input {:type "hidden" :name "name" :value entity-type}]
   [:input {:type "hidden" :name "type" :value entity-type}]
   [:button entity-type]])

(defn list-block-view [search]
  (let [blocks b/block-db
        block-names (map :name blocks)
        block-names-matching-search (if search (filter #(clojure.string/includes? (clojure.string/lower-case %) search) block-names) block-names)
        buttons (mapv add-entity-button-view block-names-matching-search)
        buttons-div (vec (cons :div buttons))]
    (conj [:div (search-block-input-view search)] buttons-div)))

(defn history-to-view [history]                             ; todo fix regex
  (reduce #(conj %1 [:li (clojure.string/replace (str %2) #"storm.stormdnd.engine.(engine|entity)/" "")]) [:ul] history))

(defn world-to-view [world-state]
  (let [world-state world-state
        entities (filter #(= :entity (:type (meta (second %)))) world-state)
        encounter-view (entity/encounter-view entities)
        history-view (history-to-view (engine/get-history))]
    [:section.container
     [:div#encounter
      (if (empty? world-state)
        [:div "Nothing in the encounter"]
        encounter-view)]
     [:div#stack history-view]]))

(defn home [_request]
  (let [world-state (engine/compute-world engine/empty-world)]
    (page
      [:head
       [:meta {:charset "UTF-8"}]
       [:title "StormD&D"]
       [:link {:rel "stylesheet" :href "css/block.css"}]
       [:script {:src "https://unpkg.com/htmx.org@1.7.0/dist/htmx.min.js" :defer true}]
       [:script {:src "https://unpkg.com/hyperscript.org@0.9.5" :defer true}]]
      [:body
       [:div#toolbar
        [:button {:hx-post "/init-mock-data" :hx-target "#main" :hx-swap "innerHTML"} "Init mock state"]
        [:button {:hx-get "/show-add-view" :hx-target "#details" :hx-swap "innerHTML"} "Add entity"]
        [:button {:hx-post (str "/mockadd/goblin") :hx-target "#main" :hx-swap "innerHTML"} "Add Goblin"]
        [:button {:hx-post "/undo" :hx-target "#main" :hx-swap "innerHTML"} "Undo"]
        [:button {:hx-put "/roll-all-initiative" :hx-target "#main" :hx-swap "innerHTML"} "Initiative"]]
       [:section.container
        [:div#details]
        [:div#main (world-to-view world-state)]]
       ])))

(defn init-mock-data []
  (engine/reset-history! mock-history)
  (world-to-view (engine/compute-world engine/empty-world)))

;; Routes
(defn ui-routes [_opts]
  [["/" {:get home}]
   ["/roll-all-initiative" {:put (fn [_req]
                                   (let [w (engine/add-to-history! `(entity/roll-all-initiative))]
                                     (ui (world-to-view w)))
                                   )}]
   ["/add" {:post (fn [{:keys [params]}]
                    (let [name (-> params :name)
                          type (-> params :type)
                          id (keyword (rand-str 5))         ; todo replace by (rand-str 5)
                          is-block-valid? (b/get-block type)]
                      (if is-block-valid?
                        (let [w (engine/add-to-history! `(entity/add-entity ~id ~name ~type))] ; todo pass name to add entity
                          (ui (world-to-view w)))
                        nil))
                    )}]
   ["/show-add-view" {:get (fn [{:keys [params]}]
                             (let [search (get params :search "")]
                               (ui (list-block-view search)))
                             )}]
   ["/mockadd/:type" {:post (fn [{:keys [path-params]}]
                              (let [type (-> path-params :type)
                                    id (keyword (rand-str 5))
                                    name (keyword (rand-str 5))
                                    w (engine/add-to-history! `(entity/add-entity ~id ~name ~type))]
                                (ui (world-to-view w)))
                              )}]
   ["/remove-entity/:id" {:post (fn [{:keys [path-params]}]
                                  (let [id (keyword (-> path-params :id))
                                        w (engine/add-to-history! `(engine/remove-from-world ~id))]
                                    (ui (world-to-view w)))
                                  )}]
   ["/damage/:id" {:put (fn [{:keys [path-params params]}]
                          (let [id (keyword (-> path-params :id))
                                hp-damage (Integer/parseInt (-> params :hp))
                                w (engine/add-to-history! `(engine/update-entity ~id (entity/damage ~hp-damage)))]
                            (ui (world-to-view w)))
                          )}]
   ["/change-name/:id" {:put (fn [{:keys [path-params params]}] ; todo refactor with set route
                               (let [id (keyword (-> path-params :id))
                                     nv (h-utils/escape-html (-> params :nv))
                                     w (engine/add-to-history! `(engine/update-entity ~id (entity/set-attribute "name" ~nv)))]
                                 (ui (world-to-view w)))
                               )}]
   ["/set/:attribute/:id" {:put (fn [{:keys [path-params params]}]
                                  (let [attribute-name (-> path-params :attribute)
                                        id (keyword (-> path-params :id))
                                        nv (Integer/parseInt (-> params :nv))
                                        w (engine/add-to-history! `(engine/update-entity ~id (entity/set-attribute ~attribute-name ~nv)))]
                                    (ui (world-to-view w)))
                                  )}]
   ["/show-block/:type" {:put (fn [{:keys [path-params]}]
                                (let [type (-> path-params :type)
                                      block (b/get-block type)]
                                  (ui (b/block-view block)))
                                )}]
   ["/undo" {:post (fn [_req] (ui (world-to-view (engine/undo!))))}]
   ["/init-mock-data" {:post (fn [_req] (ui (init-mock-data)))}]])

(def route-data
  {:muuntaja formats/instance
   :middleware
   [;; Default middleware for ui
    ;; query-params & form-params
    parameters/parameters-middleware
    ;; encoding response body
    muuntaja/format-response-middleware
    ;; exception handling
    exception/wrap-exception]})

(derive :reitit.routes/ui :reitit/routes)

(defmethod ig/init-key :reitit.routes/ui
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path route-data (ui-routes opts)])

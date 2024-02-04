(ns storm.stormdnd.engine.command
  (:require
    [storm.stormdnd.db.db :as db]
    [storm.stormdnd.engine.entity :as entity]))

(defn with-world [f]
  (fn [world]
    (let [fn-with-world (apply list (conj (vec f) world))]
      (eval fn-with-world))))
;((with-world '(add-entity :a {:hp 10})) {})
;(apply add-entity (conj [:a {:hp 10}] {}))
;(execute #(add-entity :a {:hp 1} %) [])
;(->> {}
;    (add-entity :a {:hp 10})
;    (update-entity :a (partial e/damage 10))
;    (add-entity :b {:hp 10}))
(def mock-history
  (map with-world
       (list
         '(entity/update-entity :a (partial entity/change-name 10))
         '(entity/add-entity :b {:hp 10 :name "b"})
         '(entity/update-entity :a (partial entity/damage 10))
         '(entity/add-entity :a {:hp 10})
         )))

(defn execute [command world]
  ((with-world command) world))

(defn execute-all [history world]
  ((apply comp history) world))
;(execute-all mock-history empty-world)

(def empty-world {})

(defn execute! [command]
  (let [history (db/commit! (with-world command))]
    (execute-all history empty-world)))

(defn compute-world []
  (execute-all @db/history empty-world))

(defn undo! []
  (let [_history (db/undo!)]
    (compute-world)))

(defn reset-world! []
  (db/reset-history!))

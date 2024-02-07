(ns storm.stormdnd.engine.engine
  (:require [storm.stormdnd.db.db :as db]
            [clojure.core.reducers :as r]
            [editscript.core :as es]))

(defn with-world [f & args]
  (fn [world]
    (apply f world args)))

(def with-entity with-world)

(defn add-to-world
  ([id o]
   (with-world add-to-world id o))
  ([world id o]
   (conj world {id o})))

(defn remove-from-world
  ([id]
   (with-world remove-from-world id))
  ([world id]
   (dissoc world id)))

(def empty-world {})

(defn make-world-fn [history]
  (apply comp (map eval history)))

(defn add-command-to-world-fn [pwf command]
  (comp (eval command) pwf))

(defn compute-world
  ([]
   (compute-world empty-world))
  ([world]
   (if @db/world-fn
     (@db/world-fn world)
     (compute-world @db/history world)))
  ([history world]
   (let [world-fn (make-world-fn history)]
     (world-fn world))))

(defn update-entity
  ([id f]
   (with-world update-entity id f))
  ([world id f]
   (update world id f)))

(defn add-to-history [history command]
  (conj history command))

; multi-method to dispatch if entity or world to remove the need to pass update-entity
(defn add-to-history! [command]
  (let [h (db/commit! command)]
    (db/update!
      (if @db/world-fn
        (add-command-to-world-fn @db/world-fn command)
        (do
          (print "!!! world fn was nil, recomputing from start")
          (make-world-fn h))))
    (compute-world)))

(defn reset-history!
  ([]
   (db/reset-history!)
   (compute-world))
  ([nh]
   (db/reset-history! nh)
   (compute-world)))

(defn undo! []
  (db/undo!)
  (compute-world))

(defn get-history []
  @db/history)

(defn perf-test [n]
  (let [make-mock #(reverse
                     (cons `(add-to-world :a {:hp 10000})
                           (take % (repeat `(update-entity :a (storm.stormdnd.engine.entity/damage 1))))))
        h (make-mock n)
        e (map eval h)
        ;c (time (apply comp e))
        ;red (time (r/reduce #(%2 %1) {} (reverse e)))
        ;to-add (eval `(add-entity :b {:name "b" :hp 10}))
        ;c2 (time (comp to-add c))
        ]
    ;(print "c: ")
    ;(time (apply comp e))
    (print "red: ")
    (time (r/reduce #(%2 %1) {} (reverse e)))
    ))

(def mock-history
  (let [history nil
        id :a
        damage-n 1]
    (-> history
        (add-to-history `(add-to-world ~id {:name "a" :hp 10}))
        (add-to-history `(update-entity ~id (storm.stormdnd.engine.entity/damage ~damage-n))))))

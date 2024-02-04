(ns storm.stormdnd.db.db)

(def history (atom nil))
(def world-fn (atom nil))

(defn commit! [command]
  (if (> (count command) 10000)
    (throw (AssertionError. "History full"))
    (swap! history conj command)))

(defn update! [f]
  (reset! world-fn f))

(defn reset-history!
  ([]
   (reset! history nil)
   (reset! world-fn nil))
  ([nh]
   (reset! history nh)
   (reset! world-fn nil)))

(defn undo! []
  (swap! history rest)
  (reset! world-fn nil))

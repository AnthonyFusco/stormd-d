(ns storm.stormdnd.engine.dice)
(defn parse-dice-notation [dice-notation-string]
  (if-let [[_ dice-count dice-value added-value] (re-find #"(\d+)d(\d+)([+-]\d+)?" dice-notation-string)]
    {:dice-count  (read-string dice-count)
     :dice-value  (read-string dice-value)
     :added-value (if added-value (read-string added-value) 0)}
    (throw (IllegalArgumentException. "Invalid dice notation string"))))

(defn average-dice-value [dice-notation-string]
  (let [{:keys [dice-count dice-value added-value]} (parse-dice-notation dice-notation-string)]
    (+ (-> (* dice-count (inc dice-value)) (/ 2.0) Math/ceil int)
       added-value)))

(defn roll-dice [dice-notation-string]
  (let [{:keys [dice-count dice-value added-value]} (parse-dice-notation dice-notation-string)
        results (repeatedly dice-count #(inc (rand-int dice-value)))]
    (+ (apply + results) added-value)))
(ns storm.stormdnd.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[stormdnd starting]=-"))
   :start      (fn []
                 (log/info "\n-=[stormdnd started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[stormdnd has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})

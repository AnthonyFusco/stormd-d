(ns storm.stormdnd.env
  (:require
    [clojure.tools.logging :as log]
    [storm.stormdnd.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[stormdnd starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[stormdnd started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[stormdnd has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev
                :persist-data? true}})

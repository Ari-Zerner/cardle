(ns cardle.core
  (:require [cardle.card :as card]
            [cardle.cli :as cli]))

(defn -main
  [& [answer-name]]
  (if answer-name
    (if-let [answer (card/get-card answer-name)]
      (cli/run-game answer)
      (println "Invalid card:" answer-name))
    (cli/run-game)))
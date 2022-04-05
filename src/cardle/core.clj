(ns cardle.core
  (:require [cardle.card :as card]
            [clojure.string :as str])
  (:gen-class))

(defn get-guess
  []
  (print "Guess a card (or type \"reveal\" to see the answer): ")
  (flush)
  (let [input (read-line)
        guess (if (= input "reveal") :reveal (card/get-card input))]
    (or guess (recur))))

(def field->field-name
  {:name      "Name"
   :colors    "Color"
   :types     "Type"
   :cmc       "CMC"
   :power     "Power"
   :toughness "Toughness"
   :text      "Rules Text"})

(defn- the-guess [guess _] guess)

(defn- format-words
  [words]
  (let [quoted (map #(str \" % \") words)]
    (str "Includes "
         (str/join ", " (drop-last quoted))
         (if (> 1 (count words)) ", and ")
         (last quoted))))

(def field->format-guess-comparison
  {:name      the-guess
   :colors    #(if (true? %2) %1 (str "Includes " %2 " of " %1))
   :types     #(if (true? %2) %1 (str "Includes " %2 " of " (str/join " " %1)))
   :cmc       the-guess
   :power     the-guess
   :toughness the-guess
   :text      #(format-words %2)})

(defn format-result-field
  [field [guess comparison]]
  (when comparison
    (str (field->field-name field) ": "
         ((field->format-guess-comparison field) guess comparison))))

(defn check-guess
  [guess answer]
  (let [result (card/compare-cards guess answer)]
    (println (or (not-empty (str/join "\n" (map second (card/map-fields #(format-result-field % (% result))))))
                 "No matching attributes.")))
  (= guess answer))

(defn run-game
  ([] (run-game (card/random-card)))
  ([answer]
   (println "Welcome to Cardle!")
   (loop [num-guesses 1]
     (if (check-guess (get-guess) answer)
       (println "Solved in" num-guesses "guesses!")
       (recur (inc num-guesses))))))

(defn -main
  [& [answer-name]]
  (if answer-name
    (if-let [answer (card/get-card answer)]
      (run-game answer)
      (println "Invalid card:" answer-name))
    (run-game)))
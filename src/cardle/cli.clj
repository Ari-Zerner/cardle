(ns cardle.cli
  (:require [cardle.card :as card]
            [clojure.string :as str])
  (:gen-class))

(defn get-guess
  "Get a card from the user, or the special token `:reveal`."
  []
  (print "Guess a card (or type \"reveal\" to see the answer): ")
  (flush)
  (let [input (read-line)
        guess (if (= input "reveal") :reveal (card/get-card input))]
    (or guess (do (println "Invalid card.") (recur)))))

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
         (if (> (count words) 1) ", and ")
         (last quoted))))

(def field->format-guess-comparison
  "Functions for displaying comparison results. [guess result] -> human-readable string"
  {:name      the-guess
   :colors    #(if (true? %2) %1 (str "Includes " %2 " of " %1))
   :types     #(if (true? %2) %1 (str "Includes " %2 " of " (str/join " " %1)))
   :cmc       the-guess
   :power     the-guess
   :toughness the-guess
   :text      #(format-words %2)})

(defn format-result-field
  "Formats the comparison result for a field."
  [field [guess comparison]]
  (when comparison
    (str (field->field-name field) ": "
         ((field->format-guess-comparison field) guess comparison))))

(defn check-guess
  "Given a guess and the answer, return a vector of [(whether the guess is correct) (human-readable comparison message)]."
  [guess answer]
  (let [result (card/compare-cards guess answer)
        correct (= guess answer)
        message (str (:full-card guess) "\n----\n"
                     (or (->> (card/map-fields #(format-result-field % (% result)))
                              (map second)
                              (str/join "\n")
                              not-empty)
                         "No matching attributes."))]
    [correct message]))

(defn run-game
  ([] (run-game (card/random-answer)))
  ([answer]
   (println "Welcome to Cardle!")
   (loop [num-guesses 1]
     (let [guess (get-guess)]
       (if (= :reveal guess)
         (println (:full-card answer))
         (let [[correct message] (check-guess guess answer)]
           (println message "\n")
           (if correct
             (println "Solved in" num-guesses "guesses!")
             (recur (inc num-guesses)))))))))

(defn -main
  [& [answer-name]]
  (if answer-name
    (if-let [answer (card/get-card answer-name)]
      (run-game answer)
      (println "Invalid card:" answer-name))
    (run-game)))
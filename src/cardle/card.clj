(ns cardle.card
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn- parse-rules-text
  "Turns rules text into a vector of lowercase words."
  [text]
  (->> [text]
       (mapcat #(str/split % #"\(.*?\)"))                   ; remove reminder text
       (mapcat #(str/split % #"[.,;•—]?(\s+|$)"))           ; remove punctuation and spacing
       (remove empty?)
       (map str/lower-case)
       vec))

(defn- when-val
  "(when (p x) x)"
  [p x]
  (when (p x) x))

;; A comparison is a function that, given a guessed attribute and the corresponding true attribute, returns nil if there
;; is no "match", or a "match" value, the type of which depends on the comparison.

(defn- equality-comparison
  "Matches if values are equal, returning true."
  [guess-val answer-val]
  (when (some? guess-val)
    (when-val true? (= guess-val answer-val))))

(defn- intersection-comparison
  "Matches if values have any common elements, returning the number of common elements."
  [guess-coll answer-coll]
  (or (= guess-coll answer-coll)
      (when-val pos? (count (filter (set answer-coll) guess-coll)))))

(defn- words-comparison
  "Matches if values have any words in common, returning the shared words in order of appearance in the guess."
  [guess-words answer-words]
  (->> guess-words
       (reduce (fn [[overlap remaining] word]
                 (if (pos? (get remaining word 0))
                   [(conj overlap word) (update remaining word dec)]
                   [overlap remaining]))
               [[] (frequencies answer-words)])
       first
       (when-val seq)))

(def fields [:name :colors :types :cmc :power :toughness :text])

(defn map-fields
  "Create a map of the non-nil results of calling f on the fields."
  [f]
  (->> fields
       (map #(vector % (f %)))
       (remove (comp nil? second))
       (into {})))

(defn- normalize-name
  [name]
  (apply str (interpose " " (some-> name str/lower-case (str/split #"[^a-z]+")))))

(def ^:private field->from-raw
  "Functions for getting each field from the raw data."
  {:name      #(or (% "faceName") (% "name"))
   :colors    #(str/join (sort-by {\W 0 \U 1 \B 2 \R 3 \G 4} (% "colors")))
   :types     #(str/split (or (% "type") "") #"[ —]+")
   :cmc       #(int (or (% "faceManaValue") (% "manaValue")))
   :power     #(% "power")
   :toughness #(% "toughness")
   :text      #(parse-rules-text (or (% "text") ""))})

(defn full-card-from-raw
  [{:strs [faceName name manaCost type power toughness text]}]
  (str (or faceName name) "\t" manaCost
       "\n" type
       "\n" text
       (when (and power toughness)
         (str "\n" power "/" toughness))))

(defn- process-raw-card
  "Turns a raw card record into a formatted card."
  [raw-card]
  (let [card (-> (map-fields #((field->from-raw %) raw-card))
                 (assoc :full-card (full-card-from-raw raw-card)))]
    [(normalize-name (:name card)) card]))

(defn- process-raw-cards
  "Turns the raw card records into a map of formatted cards keyed by name."
  [cards]
  (->> cards
       vals
       (apply concat)
       (map process-raw-card)
       (into {})))

(defonce ^{:doc "Map of cards keyed by name."} cards
  (-> "https://mtgjson.com/api/v5/AtomicCards.json"
      io/reader
      json/read
      (get "data")
      process-raw-cards))

(defn get-card
  [name]
  (cards (normalize-name name)))

(def answers
  "Vector of possible answer cards for the game to choose from."
  (->> (io/resource "answers.txt")
       io/reader
       line-seq
       (map #(do (when-not (get-card %) (println %)) %))
       (keep get-card)
       distinct
       vec))

(def field->comparison
  {:name      equality-comparison
   :colors    intersection-comparison
   :types     intersection-comparison
   :cmc       equality-comparison
   :power     equality-comparison
   :toughness equality-comparison
   :text      words-comparison})

(defn- compare-field
  "Applies the appropriate field comparison between guessed card and answer card."
  [field guess-card answer-card]
  (when-let [comparison ((field->comparison field) (field guess-card) (field answer-card))]
    [(field guess-card) comparison]))

(defn compare-cards
  "Applies comparisons to each field."
  [guess-card answer-card]
  (map-fields #(compare-field % guess-card answer-card)))

(defn random-card
  []
  (rand-nth (vals cards)))

(defn random-answer
  []
  (rand-nth answers))
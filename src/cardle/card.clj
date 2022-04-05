(ns cardle.card
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn- parse-rules-text
  [text]
  (->> [text]
       (mapcat #(str/split % #"\(.*?\)"))                   ; remove reminder text
       (mapcat #(str/split % #"[.,;•—]?(\s+|$)"))           ; remove punctuation and spacing
       (remove empty?)
       (map str/lower-case)
       vec))

(defn- when-val
  [p x]
  (when (p x) x))

(defn- equality-comparison
  [guess-val answer-val]
  (when (some? guess-val)
    (when-val true? (= guess-val answer-val))))

(defn- intersection-comparison
  [guess-coll answer-coll]
  (or (= guess-coll answer-coll)
      (when-val pos? (count (filter (set answer-coll) guess-coll)))))

(defn- words-comparison
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
  (apply str (-> name str/lower-case (str/split #"[^a-z ]"))))

(def ^:private field->from-raw
  {:name      #(or (% "faceName") (% "name"))
   :colors    #(str/join (sort-by {\W 0 \U 1 \B 2 \R 3 \G 4} (% "colors")))
   :types     #(str/split (or (% "type") "") #"[ —]+")
   :cmc       #(int (or (% "faceManaValue") (% "manaValue")))
   :power     #(% "power")
   :toughness #(% "toughness")
   :text      #(parse-rules-text (or (% "text") ""))})

(defn- process-raw-card
  [raw-card]
  (let [card (map-fields #((field->from-raw %) raw-card))]
    [(normalize-name (:name card)) card]))

(defn- process-raw-cards
  [cards]
  (->> cards
       vals
       (apply concat)
       (keep process-raw-card)
       (into {})))

(def cards (-> (io/resource "cards.json")                   ; "https://mtgjson.com/api/v5/AtomicCards.json"
               io/reader
               json/read
               (get "data")
               process-raw-cards))

(defn get-card
  [name]
  (cards (normalize-name name)))

(def field->comparison
  {:name      equality-comparison
   :colors    intersection-comparison
   :types     intersection-comparison
   :cmc       equality-comparison
   :power     equality-comparison
   :toughness equality-comparison
   :text      words-comparison})

(defn- compare-field
  [field guess-card answer-card]
  (when-let [comparison ((field->comparison field) (field guess-card) (field answer-card))]
    [(field guess-card) comparison]))

(defn compare-cards
  [guess-card answer-card]
  (map-fields #(compare-field % guess-card answer-card)))

(defn random-card
  []
  (rand-nth (vals cards)))
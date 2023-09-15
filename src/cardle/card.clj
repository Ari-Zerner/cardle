(ns cardle.card
  (:require
    [cardle.util :as util]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clj-http.client :as client]))

(defn- parse-rules-text
  "Turns rules text into a vector of lowercase words."
  [text]
  (->> [text]
       (mapcat #(str/split % #"\(.*?\)"))                   ; remove reminder text
       (mapcat #(str/split % #"[.,;•—]?(\s+|$)"))           ; remove punctuation and spacing
       (remove empty?)
       (map str/lower-case)
       vec))

;; A comparison is a function that, given a guessed attribute and the corresponding true attribute, returns nil if there
;; is no "match", or a "match" value, the type of which depends on the comparison.

(defn- equality-comparison
  "Matches if values are equal, returning true."
  [guess-val answer-val]
  (when (some? guess-val)
    (util/when-val true? (= guess-val answer-val))))

(defn- intersection-comparison
  "Matches if values have any common elements, returning the number of common elements, or true on a perfect match."
  [guess-coll answer-coll]
  (or (= guess-coll answer-coll)
      (util/when-val pos? (count (filter (set answer-coll) guess-coll)))))

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
       (util/when-val seq)))

(def fields [:name :colors :types :cmc :power :toughness :text])

(defn map-fields
  "Create a map of the non-nil results of calling f on the fields."
  [f]
  (->> fields
       (map #(vector % (f %)))
       (remove (comp nil? second))
       (into {})))

(def ^:private field->from-scryfall
  "Functions for getting each field from the Scryfall data."
  {:name      :name
   :colors    #(str/join (sort-by {\W 0 \U 1 \B 2 \R 3 \G 4} (:colors %)))
   :types     #(str/split (:type_line %) #"[ —]+")
   :cmc       #(int (:cmc %))
   :power     :power
   :toughness :toughness
   :text      #(parse-rules-text (:oracle_text %))})

(defn- search-scryfall
  "Search for a card using the Scryfall API. Allows for misspellings but requires a network call."
  [name]
  (let [{:keys [status body]}
        (client/get "https://api.scryfall.com/cards/named?"
                    {:query-params     {:fuzzy name}
                     :throw-exceptions false})]
    (when (= status 200) (util/read-json body))))

(defn- process-scryfall-card
  [card]
  (let [face (merge card (or (first (:card_faces card)) {}))]
    (-> (map-fields #((field->from-scryfall %) face))
        (assoc :full-card "<placeholder text>"))))

(defn- get-scryfall-card
  [name]
  (some-> name search-scryfall process-scryfall-card))

(def memoized-get-scryfall-card (memoize get-scryfall-card))

(defn get-card
  "Get a card by name. Returns nil if card isn't found."
  [name]
  (memoized-get-scryfall-card name))

(def answers
  "Vector of possible answer cards for the game to choose from."
  (->> (io/resource "answers.txt")
       io/reader
       line-seq
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

(defn random-answer
  []
  (rand-nth answers))
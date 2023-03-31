(ns cardle.util
  (:require [clojure.data.json :as json]))

(defn read-json
  "Reads JSON, transforming keys to keywords."
  [json-str]
  (json/read-str json-str :key-fn keyword))

(defn when-val
  "(when (p x) x)"
  [p x]
  (when (p x) x))
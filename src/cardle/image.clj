(ns cardle.image
  (:require
    [clj-http.client :as client]
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(defn- normalize-name
  [name]
  (-> name
      (str/replace " " "_")
      (str/replace #"[^\w]" "")
      str/lower-case))

(defn- card-image-uris
  "Get the map of Scryfall image URIs for card with exact name `name`."
  [name]
  (let [json (-> (client/get "https://api.scryfall.com/cards/named?"
                             {:query-params {:exact name}})
                 :body
                 json/read-str)]
    (if (get json "image_uris")
      (get json "image_uris")
      (let [faces (get json "card_faces")
            face (first (filter #(= (normalize-name name)
                                    (normalize-name (get % "name")))
                                faces))]
        (get face "image_uris")))))

(defn- path-for-card
  [name]
  (str (System/getenv "RESOURCES_DIR") "/images/" (normalize-name name) ".jpg"))

(defn- download-card-image
  "Download a card image from Scryfall into the directory specified by `card-images-resource`.
  Returns true if download succeeded."
  [name]
  (let [image-uri (get (card-image-uris name) "normal")]
    (when image-uri
      (with-open [in (io/input-stream image-uri)
                  out (io/output-stream (path-for-card name))]
        (io/copy in out)
        true))))

(defn cached-image
  "Return the relative path to the image for card `name`, downloading it if needed, or nil if there is no image for `name`"
  [name]
  (let [path (path-for-card name)]
    (if (.exists (io/file path))
      path
      (when (download-card-image name) path))))
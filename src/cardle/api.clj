(ns cardle.api
  (:use [compojure.core])
  (:require [cardle.card :as card]
            [cardle.cli :as cli]
            [cardle.image :as image]
            [clojure.java.io :as io]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.json :as ring-json]
            [ring.middleware.params :as ring-params]))

(defn handle-start
  []
  {:status 200
   :body {:answer (:name (card/random-answer))}})

(defn handle-guess
  [guess-name answer-name]
  (if-let [guess (card/get-card (if (= guess-name "devreveal") answer-name guess-name))]
    (if-let [answer (card/get-card answer-name)]
      (let [[correct message] (cli/check-guess guess answer)]
        {:status 200
         :body   {:correct correct
                  :guess guess
                  :answer answer
                  :message message}})
      {:status 404
       :body (str "Card not found: " answer-name)})
    {:status 404
     :body (str "Card not found: " guess-name)}))

(defn handle-image
  [name]
  (if-let [image-path (image/cached-image name)]
    (-> {:status 200
         :body   (io/input-stream image-path)}
        (ring.util.response/content-type "image/jpeg"))
    {:status 404
     :body (str "Card not found: " name)}))

(defroutes
  api-routes
  (context "/api" []
    (GET "/" [] "Sorry, this API doesn't have documentation yet. https://github.com/Ari-Zerner/cardle")
    (GET "/start" [] (handle-start))
    (POST "/guess" {{:keys [guess answer]} :params} (handle-guess guess answer))
    (GET "/image" {{:keys [name]} :params} (handle-image name))))

(defn api-middleware
  [routes]
  (-> routes
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      ring-json/wrap-json-response
      ring-params/wrap-params))
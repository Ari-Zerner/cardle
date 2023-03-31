(ns cardle.server
  (:use [compojure.core])
  (:require [cardle.card :as card]
            [cardle.cli :as cli]
            [compojure.route :as route]
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
                  :message message}})
      {:status 404
       :body (str "Card not found: " answer-name)})
    {:status 404
     :body (str "Card not found: " guess-name)}))

(defroutes
  api-routes
  (context "/api" []
    (GET "/" [] "Sorry, this API doesn't have documentation yet. https://github.com/Ari-Zerner/cardle")
    (GET "/start" [] (handle-start))
    (POST "/guess" {{:keys [guess answer]} :params} (handle-guess guess answer)))
  (route/not-found "Endpoint not found"))

(def handler
  (-> api-routes
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      ring-json/wrap-json-response
      ring-params/wrap-params))
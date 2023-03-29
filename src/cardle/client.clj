(ns cardle.client
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [hiccup.core :as hiccup]
            [clj-http.client :as client]
            [ring.middleware.params :as ring-params]
            [ring.middleware.resource :as ring-resource]))

(defn- api-url
  [route]
  (str "http://localhost:8080/api" route))

(defn- get-answer
  []
  (some-> (api-url "/start") client/get :body json/read-str (get "answer")))

(defn- compare-cards
  [guess answer]
  (let [{:keys [status body]} (client/post (api-url "/guess")
                              {:form-params {:guess guess :answer answer}
                               :throw-exceptions false})]
    (case status
      200
      {:status  :ok
       :correct (-> body json/read-str (get "correct"))
       :message (-> body json/read-str (get "message"))}
      404
      {:status  :not-found
       :message (str "The card you guessed (" guess ") was not recognized.")}

      {:status  :error
       :message body
       :code    status})))

(defn- render-head
  [& [subtitle]]
  [:head
   [:title (str "Cardle" (when subtitle (str " – " subtitle)))]
   [:link {:rel "stylesheet" :type "text/css" :href "/css/styles.css"}]])

(defn- render-win-page
  [message]
  (hiccup/html
    [:html
     (render-head "You Win!")
     [:body
      [:h1 "Cardle"]
      [:p "Congratulations, you win!"]
      (when message [:p (str/replace message "\n" "<br>")])
      [:form {:method "get" :action "/"}
       [:input {:type "submit" :value "New Game"}]]]]))

(defn- render-error-page
  [message]
  (hiccup/html
    [:html
     (render-head "Error")
     [:body
      [:h1 "Cardle"]
      [:p message]
      [:form {:method "get" :action "/"}
       [:input {:type "submit" :value "New Game"}]]]]))

(defn- render-guess-page
  [answer message]
  (if answer
    (hiccup/html
      [:html
       (render-head)
       [:body
        [:h1 "Cardle"]
        [:form {:method "post" :action "/"}
         [:label {:for "guess"} "Guess: "]
         [:input {:type "text" :name "guess" :id "guess"}]
         [:input {:type "hidden" :name "answer" :id "answer" :value answer}]
         [:input {:type "hidden" :name "message-log" :id "message-log" :value message}]
         [:br]
         [:input {:type "submit" :value "Submit"}]
         (when message [:p (str/replace message "\n" "<br>")])]]])
    (render-error-page "Failed to get answer from server.")))

(defn- handle-guess
  [params]
  (let [{:strs [guess answer message-log]} params
        {:keys [status message correct]} (compare-cards guess answer)]
    (cond
      (= status :error) (render-error-page message)
      correct           (render-win-page message)
      :else             (render-guess-page answer (str message "\n\n\n" message-log)))))

(defroutes app-routes
           (GET "/" [] (render-guess-page (get-answer) nil))
           (POST "/" {:keys [params]} (handle-guess params)))

(def handler
  (-> app-routes
      ring-params/wrap-params
      (ring-resource/wrap-resource "public")))

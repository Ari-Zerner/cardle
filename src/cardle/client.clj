(ns cardle.client
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [hiccup.core :as hiccup]
            [clj-http.client :as client]
            [ring.middleware.params :as ring-params]
            [ring.middleware.resource :as ring-resource]
            [ring.middleware.session :as ring-session]))

(defn- api-url
  [route]
  (str "http://localhost:8080/api" route))

(defn- get-answer
  []
  (some-> (api-url "/start") client/get :body json/read-str (get "answer")))

(defn- compare-cards
  [guess answer]
  (let [{:keys [status body]} (client/post (api-url "/guess")
                                           {:form-params      {:guess guess :answer answer}
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

(defn- with-session
  [new-session body]
  {:session new-session
   :body    body})

(defn- render-head
  [& [subtitle]]
  [:head
   [:title (str "Cardle" (when subtitle (str " – " subtitle)))]
   [:link {:rel "stylesheet" :type "text/css" :href "/css/styles.css"}]])

(defn- render-message-log
  [session]
  (let [[last-result & results] (:results session)]
    [:p (str/replace (->> results
                          (filter #(= :ok (:status %)))
                          (concat [last-result])
                          (map :message)
                          (str/join "\n\n\n"))
                     "\n" "<br>")]))

(defn- render-win-page
  [session]
  (with-session
    {}
    (hiccup/html
      [:html
       (render-head "You Win!")
       [:body
        [:h1 "Cardle"]
        [:p "Congratulations, you win!"]
        (render-message-log session)
        [:form {:method "get" :action "/"}
         [:input {:type "submit" :value "New Game"}]]]])))

(defn- render-error-page
  [message]
  (with-session
    {}
    (hiccup/html
      [:html
       (render-head "Error")
       [:body
        [:h1 "Cardle"]
        [:p message]
        [:form {:method "get" :action "/"}
         [:input {:type "submit" :value "New Game"}]]]])))

(defn- render-guess-page
  [session]
  (if (:answer session)
    (with-session
      session
      (hiccup/html
        [:html
         (render-head)
         [:body
          [:h1 "Cardle"]
          [:form {:method "post" :action "/"}
           [:label {:for "guess"} "Guess: "]
           [:input {:type "text" :name "guess" :id "guess"}]
           [:br]
           [:input {:type "submit" :value "Submit"}]
           (render-message-log session)]]]))
    (render-error-page "Failed to get answer from server.")))

(defn- handle-entry
  [session]
  (render-guess-page
    (if (empty? session)
      {:answer (get-answer), :results ()}
      session)))

(defn- handle-guess
  [guess {:keys [answer] :as session}]
  (let [{:keys [status message correct] :as result} (compare-cards guess answer)
        session (update session :results #(conj % result))]
    (cond
      (= status :error) (render-error-page message)
      correct (render-win-page session)
      :else (render-guess-page session))))

(defroutes app-routes
           (GET "/" {:keys [session]} (handle-entry session))
           (POST "/" {:keys [params session]} (handle-guess (get params "guess") session)))

(def handler
  (-> app-routes
      ring-params/wrap-params
      (ring-session/wrap-session {:cookie-name "cardle-game"})
      (ring-resource/wrap-resource "public")))

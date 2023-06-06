(ns cardle.webapp
  (:use [compojure.core])
  (:require [cardle.api :as api]
            [cardle.app :as app]
            [ring.middleware.resource :as ring-resource]))

(def handler
  (-> (routes
        (wrap-routes api/api-routes api/api-middleware)
        (wrap-routes app/app-routes app/app-middleware))
      (ring-resource/wrap-resource "public")))
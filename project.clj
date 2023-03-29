(defproject cardle "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "2.4.0"]
                 [ring "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.7.0"]
                 [clj-http/clj-http "3.12.3"]]
  :source-paths     ["src"]
  :watch-dirs       ["src"]
  :profiles {:cli    {:main cardle.cli}
             :server {:plugins [[lein-ring "0.12.6"]]
                      :ring    {:handler cardle.server/handler}}
             :client {:plugins [[lein-ring "0.12.6"]]
                      :ring    {:handler cardle.client/handler}}}
  :aliases {"run-cli"    ["with-profile" "cli" "run"]
            "run-server" ["with-profile" "server" "ring" "server-headless" "8080"]
            "run-client" ["with-profile" "client" "ring" "server" "3000"]})

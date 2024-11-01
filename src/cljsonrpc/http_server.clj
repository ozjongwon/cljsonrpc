(ns cljsonrpc.http-server
  (:require
   [aleph.http :as http]
   [byte-streams :as bs]
   [cljsonrpc.core :as c]
   [clojure.data.json :as json]
   [ring.middleware.json :as rmj]
   [ring.util.request :as rurq]
   [ring.util.response :as rurs]))

;;(remove-ns 'cljsonrpc.http-server)

(defn- req-handler 
  [f]
  (fn [req]
    (let [json-str (rurq/body-string req)]
      (rurs/response (f json-str)))))

(defn json-rpc-handler
  [f]
  (rmj/wrap-json-response (#'req-handler f)))

(defn start-server
  [handler port]
  (http/start-server handler {:port port
                              ;; :http-versions [:http2 :http1]
                              ;; :insecure? true
                              ;; :use-h2c? true
                              ;; :compression? true
                              }))

(defn stop-server [server]
  (.close server))

(comment
  ;; Start the server
  (def json-rpc-server (start-server (json-rpc-handler #(c/process-json-rpc %))
                                     8888))

  (stop-server json-rpc-server)

  (defn make-rpc-request [url method params]
    (http/post url {:content-type :json
                    :body (json/write-str
                           {:jsonrpc "2.0"
                            :method method
                            :params params
                            :id (str (java.util.UUID/randomUUID))})}))

  (-> @(make-rpc-request "http://localhost:8888" "+" [1 2 3])
      :body
      bs/to-string
      json/read-str)

  (-> @(make-rpc-request "http://localhost:8888" "*" [2 3 4])
      :body
      bs/to-string
      json/read-str)

  (-> @(make-rpc-request "http://localhost:8888" "-" [10 5 2])
      :body
      bs/to-string
      json/read-str)

  (-> @(make-rpc-request "http://localhost:8888" "/" [100 2 2])
      :body
      bs/to-string
      json/read-str))

;; curl -X POST http://localhost:8888 -H 'Content-Type: application/json' -d "{\"method\":\"+\",\"params\":[1,2,3],\"id\":\"id1\"}"

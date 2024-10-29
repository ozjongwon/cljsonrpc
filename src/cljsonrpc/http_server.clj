(ns cljsonrpc.http-server
  (:require [aleph.http :as http]
            [ring.util.request :as rurq]
            [ring.util.response :as rurs]
            [ring.middleware.json :as rmj]
            ;; [cljsonrpc.core :as c]
            ))
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

#_
(def json-rpc-server (start-server (json-rpc-handler #(c/process-json-rpc %))
                                   8888))

#_ (stop-server json-rpc-server)
;; curl -X POST http://localhost:8888 -H 'Content-Type: application/json' -d "{\"method\":\"+\",\"params\":[1,2,3],\"id\":\"id1\"}"

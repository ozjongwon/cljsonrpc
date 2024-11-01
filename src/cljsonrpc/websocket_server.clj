(ns cljsonrpc.websocket-server
  (:require
   [aleph.http :as http]
   [cljsonrpc.core :as c]
   [cljsonrpc.http-server :refer [start-server stop-server]]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [manifold.deferred :as d]
   [manifold.stream :as s]))

(defn json-rpc-handler
  [f]
  (fn [req]
    (-> (http/websocket-connection req)
        (d/chain (fn [socket]
                   (s/connect-via socket
                                  (fn [msg]
                                    (try
                                      (let [response (f msg)]
                                        (s/put! socket response))
                                      (catch Exception e
                                        (log/error e "Uhandled Error!")
                                        (s/put! socket
                                                (json/write-str
                                                 {:jsonrpc "2.0"
                                                  :error {:code -32603
                                                          :message "Internal server error"}})))))
                                  socket))))))


;; Example usage
(comment

  ;; Client-side code
  (defn make-rpc-request [url method params]
    (d/let-flow
        [conn (http/websocket-client url)
         request (json/write-str
                  {:jsonrpc "2.0"
                   :method method
                   :params params
                   :id (str (java.util.UUID/randomUUID))})
         _ (s/put! conn request)
         response (s/take! conn)]
      (try
        (let [parsed (json/read-str response)]
          (if (:error parsed)
            (throw (ex-info "RPC error" (:error parsed)))
            parsed))
        (catch Exception e
          (log/error e "Client side error!"))
        (finally
          (s/close! conn)))))

  ;; Start the server

  (def json-rpc-server (start-server (json-rpc-handler #(when-let [result (c/process-json-rpc %)]
                                                          (json/write-str result))) 8888))
  (stop-server json-rpc-server)

  ;; Make requests
  (try
    @(make-rpc-request "ws://localhost:8888" "+" [1 2 3])    ;; Should return 6
    (catch Exception e
      (println "Error:" (ex-message e))))

  ;; Multiple operations
  @(make-rpc-request "ws://localhost:8888" "*" [2 3 4])    ;; Should return 24
  @(make-rpc-request "ws://localhost:8888" "-" [10 5 2])   ;; Should return 3
  @(make-rpc-request "ws://localhost:8888" "/" [100 2 2])  ;; Should return 25

  ;; Clean up
  (.close server))

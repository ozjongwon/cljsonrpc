(ns cljsonrpc.core
  (:require [cljsonrpc.request-response :as jr]
            [clojure.data.json :as json]
            [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [cljsonrpc.server :as svr]))

(defonce predefined-errors
  {:parse-error {:code -32700 :message "Invalid JSON was received by the server."}
   :invalid-request {:code -32600 :message "The JSON sent is not a valid Request object."}
   :method-not-found {:code -32601 :message "The method does not exist / is not available."}
   :invalid-params {:code -32602 :message "Invalid method parameter(s)."}
   :internal-error  {:code -32603 :message "Internal JSON-RPC error."}})

(def user-defined-errors {})

(def fn-ns-list '[clojure.core cljsonrpc.core])

(defn- find-fn [fn-name]
  (loop [[ns & rest] fn-ns-list]
    (let [resolved (->> fn-name
                        symbol
                        (ns-resolve (find-ns ns)))]
      (if (ifn? resolved)
        resolved
        (recur rest)))))


(defn read-json [str]
  (try (json/read-str str)
       (catch Exception _
         (throw (ex-info "Parse error!" {:code :parse-error})))))

(defn process-request [m]
  (let [{version "jsonrpc" method "method" params "params" id "id"} (when (map? m)
                                                                      m)]
    (try (cond (or (and version (not= version jr/json-rpc-2))
                   (nil? method)
                   (not (vector? params)))
               (throw (ex-info "Invalid request!" {:code :invalid-request :data m}))

               :else
               (if-let [f (find-fn method)]
                 (when id ;; not notification
                   (jr/make-response (apply f params) id version))
                 (throw (ex-info "Method not found!" {:code :method-not-found :data m}))))
         ;; code	message	meaning
         ;; -32000 to -32099	Server error	Reserved for implementation-defined server-errors.

         (catch clojure.lang.ExceptionInfo e
           (when id
             (let [{:keys [code data]} (ex-data e)]
               (-> (let [{:keys [code message]} (or (get predefined-errors code)
                                                    (get user-defined-errors code))]
                     {:data data
                      :code code
                      :message (or message (ex-message e))})
                   jr/map->JsonRpcError
                   (jr/make-response id version)))))
         (catch Exception e
           (when id
             (-> (:internal-error predefined-errors)
                 (assoc :data m)
                 jr/map->JsonRpcError
                 (jr/make-response id version)))))))

(defn process-requests [v]
  (mapv process-request v))

(defn process-json-rpc
  [json-str]
  (try (let [parsed-json (read-json json-str)]
         (if (vector? parsed-json) ;; batch
           (process-requests parsed-json)
           (process-request parsed-json)))
       (catch clojure.lang.ExceptionInfo e ;; parse-error
         (let [{:keys [code]} (ex-data e)]
           (-> (let [{:keys [code message]} (get predefined-errors code)]
                 {:data json-str
                  :code code
                  :message (ex-message e)})
               jr/map->JsonRpcError
               (jr/make-response nil nil))))))

;;;
;;;
;;;
(comment
  (defn client
    [host port]
    (d/chain (tcp/client {:host host, :port port})
             #(svr/wrap-duplex-stream svr/protocol %)))

  (def json-rpc-server (svr/start-server (svr/json-rpc-handler #(when-let [result (process-json-rpc %)]
                                                                  (json/write-str result)))
                                         8888))
  (def c @(client "localhost" 8888))
  (.close json-rpc-server)

  @(s/put! c (json/write-str (jr/make-request "+" (list  1 2 3) :id "id1")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "+" (list  1 2 3) :id "id1" :jsonrpc "2.0")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "cx" (list  1 2 3) :id "id1")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "cx" (list  1 2 3) :id "id1" :jsonrpc "2.0")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "+" "params" :id "id1")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "+" "params" :id "id1" :jsonrpc "2.0")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "(" "params" :id "id1")))
  @(s/take! c)

  @(s/put! c (json/write-str (jr/make-request "(" "params" :id "id1" :jsonrpc "2.0")))
  @(s/take! c)

  (defn foo [& args]
    (+ "a"))

  @(s/put! c (json/write-str (jr/make-request "foo" [1 2 3] :id "id1" :jsonrpc "2.0")))
  @(s/take! c)

  @(s/put! c (json/write-str [(jr/make-request "+" (list  1 2 3) :id "id1")
                              (jr/make-request "+" (list  1 2 3) :id "id1" :jsonrpc "2.0")]))
  @(s/take! c)

  ;; notification
  @(s/put! c (json/write-str (jr/make-request "+" [1 2 3])))
  @(s/take! c)
  )

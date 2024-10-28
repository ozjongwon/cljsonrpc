(ns cljsonrpc.server
  (:require [aleph.tcp :as tcp]
            [gloss.core :as gloss]
            [gloss.io :as io]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.data.json :as json]
            [cljsonrpc.request-response :as jr]))


(defonce protocol (gloss/compile-frame (gloss/finite-frame :uint32
                                                           (gloss/string :utf-8))
                                       json/write-str
                                       json/read-str))


(defn wrap-duplex-stream
  [protocol stream]
  (let [out (s/stream)]
    (s/connect (s/map #(io/encode protocol %) out)
               stream)

    (s/splice out
              (io/decode-stream stream protocol))))

;; (defn client
;;   [host port]
;;   (d/chain (tcp/client {:host host, :port port})
;;            #(wrap-duplex-stream protocol %)))

(defn json-rpc-handler
  [f]
  (fn [stream info]
    (s/connect (s/map f stream) stream)))

(defn start-server
  [handler port]
  (tcp/start-server (fn [stream info]
                      (handler (wrap-duplex-stream protocol stream) info))
                    {:port port}))

(defn process-json-rpc
  [m]
  (let [{version "jsonrpc" method "method" params "params" id "id"} m
        resolved (->> method
                      symbol
                      resolve)]
    (if (and resolved (->> resolved
                           deref
                           ifn?))
      (jr/make-response (apply resolved params) id version)
      ;; FIXME try-catch
      :no-such-fn)))

(def json-rpc-server (start-server (json-rpc-handler process-json-rpc) 8888))

;;;
;;;
;;;
(defn client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream protocol %)))

(def c @(client "localhost" 8888))

@(s/put! c (jr/make-request "+" (list  1 2 3) "id1"))
@(s/take! c)

@(s/put! c (jr/make-request "+" (list  1 2 3) "id1" "2.0"))
@(s/take! c)

@(s/put! c (jr/make-request "cx" (list  1 2 3) "id1"))
@(s/take! c)

@(s/put! c (jr/make-request "cx" (list  1 2 3) "id1" "2.0"))
@(s/take! c)


(.close json-rpc-server)

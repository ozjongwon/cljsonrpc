(ns cljsonrpc.server
  (:require [aleph.tcp :as tcp]
            [gloss.core :as gloss]
            [gloss.io :as io]
            [manifold.stream :as s]))

(defonce protocol (gloss/compile-frame (gloss/finite-frame :uint32
                                                           (gloss/string :utf-8))
                                       ;; json/write-str
                                       ;; json/read-str
                                       ))


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

(defn stop-server [server]
  (.close server))

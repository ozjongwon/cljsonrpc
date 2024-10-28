(ns cljsonrpc.client
  (:require [org.httpkit.server :as httpkit]))

(require
 '[aleph.http :as http]
 '[manifold.deferred :as d]
 '[clj-commons.byte-streams :as bs])

(-> @(http/get "https://google.com/")
    :body
    bs/to-string
    prn)

(d/chain (http/get "https://google.com")
         :body
         bs/to-string
         prn)

;; To support HTTP/2, do the following:
(def conn-pool
  (http/connection-pool {:connection-options {:http-versions [:http2 :http1]}}))
@(http/get "https://google.com" {:pool conn-pool})
;; See aleph.examples.http2 for more details

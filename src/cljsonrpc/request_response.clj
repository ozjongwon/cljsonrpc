(ns cljsonrpc.request-response
  (:require [clojure.data.json :as json]
            [medley.core :as med]))

(defonce json-rpc-2 "2.0")

#_(defn make-request
    ([method params id]
     (make-request method params id nil))
    ([method params id version]
     (cond-> {:method method :params params :id id}
       (= json-rpc-2 version) (assoc :jsonrpc version))))

(defn make-request
  [method params & {:keys [id jsonrpc] :as opts}]
  (cond-> {:method method :params params :id id}
    (= json-rpc-2 jsonrpc) (assoc :jsonrpc jsonrpc)))

;; (defn ->clj
;;   [message]
;;   (let [{version "jsonrpc" method "method" params "params" id "id"}
;;         (json/read-str message)
;;         resolved (->> method
;;                       symbol
;;                       resolve)]
;;     (if (and resolved (->> resolved
;;                            deref
;;                            ifn?))
;;       (apply resolved params)
;;       :no-such-fn)))

(defprotocol ResponseResult 
  (make-response [this id version]))

(extend-protocol ResponseResult
  java.lang.Object
  (make-response [this id version]
    (cond-> {:result this :id id}
      (= json-rpc-2 version) (assoc :jsonrpc version)
      (not= json-rpc-2 version) (assoc :error nil))))

(defrecord JsonRpcError [code message data]
  ResponseResult
  (make-response [this id version]
    (cond-> {:error (if code ;; v2
                      (med/remove-vals nil? this)
                      message)
             :id id}
      (= json-rpc-2 version) (assoc :jsonrpc version)
      (not= json-rpc-2 version) (assoc :result nil))))

;; (defn clj->
;;   ([message id]
;;    (clj-> message id nil))
;;   ([message id version]
;;    (-> message
;;        (make-response id version)
;;        json/write-str)))

;; C-(		paredit-backward-slurp-sexp
;; C-)		paredit-forward-slurp-sexp
;; C-{		paredit-backward-barf-sexp
;; C-}		paredit-forward-barf-sexp
;; M-<down>	paredit-splice-sexp-killing-forward
;; M-<up>		paredit-splice-sexp-killing-backward
;; <delete>	paredit-forward-delete
;; <deletechar>	paredit-forward-delete

;; C-M-b		paredit-backward
;; C-M-d		paredit-forward-down
;; C-M-f		paredit-forward
;; C-M-n		paredit-forward-up
;; C-M-p		paredit-backward-down
;; C-M-u		paredit-backward-up
;; M-"		paredit-meta-doublequote
;; M-(		paredit-wrap-round
;; M-)		paredit-close-round-and-newline
;; M-;		paredit-comment-dwim
;; M-J		paredit-join-sexps
;; M-S		paredit-split-sexp
;; M-d		paredit-forward-kill-word
;; M-q		paredit-reindent-defun
;; M-r		paredit-raise-sexp
;; M-DEL		paredit-backward-kill-word
;; ESC C-<left>	paredit-backward-slurp-sexp
;; ESC C-<right>	paredit-backward-barf-sexp
;; ESC <down>	paredit-splice-sexp-killing-forward
;; ESC <up>	paredit-splice-sexp-killing-backward

(ns cljsonrpc.request-response
  (:require [clojure.data.json :as json]
            [medley.core :as med]))

(defonce json-rpc-2 "2.0")

(defn make-request
  ([method params id]
   (make-request method params id nil))
  ([method params id version]
   (cond-> {:method method :params params :id id}
     (= json-rpc-2 version) (assoc :jsonrpc version)
     :finally json/write-str)))

(defn ->clj
  [message]
  (let [{version "jsonrpc" method "method" params "params" id "id"}
        (json/read-str message)
        resolved (->> method
                      symbol
                      resolve)]
    (if (and resolved (->> resolved
                           deref
                           ifn?))
      (apply resolved params)
      :no-such-fn)))

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

(defn clj->
  ([message id]
   (clj-> message id nil))
  ([message id version]
   (-> message
       (make-response id version)
       json/write-str)))

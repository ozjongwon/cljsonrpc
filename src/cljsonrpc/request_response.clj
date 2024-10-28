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

(defn make-request
  ([method params id]
   (make-request method params id nil))
  ([method params id version]
   (-> {:method method :params params :id id}
       (med/assoc-some :jsonrpc version)
       json/write-str)))

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

(defn make-response
  ([message context]
   (make-response message context false))
  ([message context error?]
   ;; context - {:jsonrpc version :id id}
   (json/write-str (assoc context
                          (if error?
                            :error
                            :result)
                          message))))
(defn clj->
  ([message id]
   (clj-> message id nil))
  ([message id version]
   (->> version
        (med/assoc-some {:result message :id id} :jsonrpc)
        (make-response message))))

(ns cljsonrpc.request-response
  (:require [cljsonrpc.request-response :as sut]
            [clojure.test :as t]
            [clojure.data.json :as json]))

(testing "Call through JSON RPC request"
  (t/is (= 6 (->clj (make-request "+" (list  1 2 3) "id1"))))
  (t/is (= 6 (->clj (make-request "+" (list  1 2 3) "id1" "2.0")))))

(testing "Carete RPC response"
  (t/is (= {"id" "id1", "result" "result1" "error" nil}  (json/read-str (clj-> "result1" "id1"))))
  (t/is (= {"jsonrpc" "2.0" "id" "id1", "result" "result1"} (json/read-str (clj-> "result1" "id1" "2.0")))))

(make-response (map->JsonRpcError {:data "123" :code 2 :foo nil}) 111 "2.0")
(make-response (map->JsonRpcError {:data "123" :message 2 :foo nil}) 111 nil)
(testing "Carete RPC error response"
  (t/is (= {"id" "id1", "error" "result1"}  (json/read-str (clj-> "result1" "id1"))))
  (t/is (= {"jsonrpc" "2.0" "id" "id1", "error" "result1"} (json/read-str (clj-> "result1" "id1" "2.0")))))




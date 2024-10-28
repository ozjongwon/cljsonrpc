(ns cljsonrpc.request-response
  (:require [cljsonrpc.request-response :as sut]
            [clojure.test :as t]
            [clojure.data.json :as json]))

(testing "Call through JSON RPC request"
  (t/is (= 6 (->clj (sut/make-request "+" (list  1 2 3) "id1"))))
  (t/is (= 6 (->clj (make-request "+" (list  1 2 3) "id1" "2.0")))))

(testing "Carete RPC response"
  (t/is (= {"id" "id1", "result" "result1"}  (json/read-str (clj-> "result1" "id1"))))
  (t/is (= {"jsonrpc" "2.0" "id" "id1", "result" "result1"} (json/read-str (clj-> "result1" "id1" "2.0")))))




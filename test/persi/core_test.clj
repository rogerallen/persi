(ns persi.core-test
  (:use clojure.test)
  (:require [persi.core :as persi]))

(defn test-init []
  (println "test-init")
  (persi/init! "test/persi-test-dir" false)
  (println "test-init-done"))

(deftest test-single-event
  (testing "test-single-event"
    (test-init)
    (persi/add-event! true)
    (is (= (first (persi/events) true)))))

(deftest test-save-single-event
  (testing "test-save-single-event"
    (test-init)
    (let [cur-file-name (persi/get-file-name)]
          (persi/add-event! true)
          (persi/save)
          (persi/new)
          (persi/open cur-file-name)
          (is (= (first (persi/events) true))))))

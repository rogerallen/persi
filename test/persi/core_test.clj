(ns persi.core-test
  (:use clojure.test)
  (:require [persi.core :as persi]))

(defn test-init []
  (println "test-init")
  (persi/init! "test/persi_test_dir" false)
  (persi/new)
  (println "test-init-done"))

(deftest test-single-event
  (testing "test-single-event"
    (test-init)
    (println "test-single-event")
    (persi/add-event! true)
    (is (= (first (persi/events)) true))))

(deftest test-save-single-event
  (testing "test-save-single-event"
    (test-init)
    (println "test-save-single-event")
    (let [cur-file-name (persi/get-file-name)
          _ (println "cur-file-name" cur-file-name)]
          (persi/add-event! true)
          (persi/save)
          (persi/new)
          (persi/open cur-file-name)
          (is (= (first (persi/events)) true)))))

;;(deftest test-in-order
;;  (test-single-event)
;;  (test-save-single-event))

;;(defn test-ns-hook []
;;  (test-in-order))


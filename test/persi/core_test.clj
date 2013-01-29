;; run tests:
;; rm -rf persi_files* test/persi_test_dir* && lein test
;; rm -rf persi_files* test/persi_test_dir* && lein cloverage
;;
(ns persi.core-test
  (:use clojure.test)
  (:require [persi.core :as persi]))

(defn test-init []
  (persi/init! "test/persi_test_dir" false)
  (persi/new))

(defn test-init2 []
  (persi/init! "persi_files_test" false)
  (persi/new))

(deftest test-single-event
  (testing "test-single-event"
    (test-init)
    (persi/add-event! true)
    (is (= (persi/events) [true]))))

(deftest test-save-single-event
  (testing "test-save-single-event"
    (test-init)
    (let [cur-file-name (persi/get-file-name)]
          (persi/add-event! true)
          (persi/save)
          (persi/new)
          (persi/open cur-file-name)
          (is (= (persi/events) [true])))))

(deftest test-save-several-events
  (testing "test-save-several-events"
    (test-init)
    (let [cur-file-name (persi/get-file-name)]
          (persi/add-event! 1)
          (persi/add-event! 2)
          (persi/add-event! 3)
          (persi/save)
          (persi/save) ;; redundant 
          (persi/new)
          (persi/add-event! false)
          (persi/add-event! true)
          (persi/add-event! false)
          (is (= (persi/events) [false true false]))
          (persi/open cur-file-name)
          (is (= (persi/events) [1 2 3])))))

(deftest test-default-mode
  (testing "test-default-mode"
    (persi/init!)
    (persi/new)
    (persi/add-event! true)
    (persi/summary) ;; just for coverage?
    (is (= "persi_files" (persi/get-dir-name)))
    (is (= (persi/events) [true]))))

(deftest test-default-mode-again
  (testing "test-default-mode-again"
    (persi/init!)
    (persi/new)
    (persi/add-event! 3)
    (is (= (persi/events) [3]))))

(deftest test-single-event2
  (testing "test-single-event2"
    (test-init2)
    (persi/add-event! true)
    (is (= (persi/events) [true]))))

(deftest test-save-single-event2
  (testing "test-save-single-event"
    (test-init2)
    (let [cur-file-name (persi/get-file-name)]
          (persi/add-event! true)
          (persi/save)
          (persi/new)
          (persi/open cur-file-name)
          (is (= (persi/events) [true])))))





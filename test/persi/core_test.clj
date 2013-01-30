;; run tests:
;; rm -rf persi_files* test/persi_test_dir* && lein test
;; rm -rf persi_files* test/persi_test_dir* && lein cloverage
;;
(ns persi.core-test
  (:use clojure.test)
  (:require [persi.core :as persi]))

(defn test-init []
  (is (= nil (persi/init! "test/persi_test_dir" false)))
  (persi/new!))

(defn test-init2 []
  (is (= nil (persi/init! "persi_files_test" false)))
  (persi/new!))

(deftest test-single-append
  (testing "test-single-append"
    (test-init)
    (persi/append! true)
    (is (= (persi/get-list) [true]))))

(deftest test-save-single-append
  (testing "test-save-single-append"
    (test-init)
    (let [cur-file-name (persi/get-file-name)]
          (persi/append! true)
          (is (= true (persi/save!)))
          (persi/new!)
          (is (= nil (persi/open! cur-file-name)))
          (is (= (persi/get-list) [true])))))

(deftest test-save-several-appends
  (testing "test-save-several-appends"
    (test-init)
    (let [cur-file-name (persi/get-file-name)]
          (persi/append! 1)
          (persi/append! 2)
          (persi/append! 3)
          (is (= true (persi/save!)))
          (is (= false (persi/save!))) ;; redundant 
          (persi/new!)
          (persi/append! false)
          (persi/append! true)
          (persi/append! false)
          (is (= (persi/get-list) [false true false]))
          (is (= nil (persi/open! cur-file-name)))
          (is (= (persi/get-list) [1 2 3])))))

(deftest test-default-mode
  (testing "test-default-mode"
    (persi/init!)
    (persi/new!)
    (persi/append! true)
    (persi/insert! :a 1)
    (persi/summary) ;; just for coverage?
    (is (= true (persi/save!)))
    (is (= persi/persi-default-dir-name (persi/get-dir-name)))
    (is (= (persi/get-list) [true]))
    (is (= (persi/get-map) {:a 1}))))

(deftest test-default-mode-again
  (testing "test-default-mode-again"
    (persi/init!)
    (is (= persi/persi-default-dir-name (persi/get-dir-name)))    
    (persi/new! "hi_there.clj")
    (persi/append! 3)
    (is (= true (persi/save!)))
    (is (= (persi/get-list) [3]))))

(deftest test-single-append2
  (testing "test-single-append2"
    (test-init2)
    (persi/append! true)
    (is (= (persi/get-list) [true]))))

(deftest test-save-single-append2
  (testing "test-save-single-append"
    (test-init2)
    (let [cur-file-name (persi/get-file-name)]
          (persi/append! true)
          (is (= true (persi/save!)))
          (persi/new!)
          (persi/open! cur-file-name)
          (is (= (persi/get-list) [true])))))

;; these tests cannot run in parallel
(deftest test-serially
  (test-single-append)
  (test-save-single-append)
  (test-save-several-appends)
  (test-default-mode)
  (test-default-mode-again)
  (test-single-append2)
  (test-save-single-append2))

(defn test-ns-hook []
  (test-serially))




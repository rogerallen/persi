(ns persi.core
  (:require [clojure.java.io :as cio]))

;; ======================================================================
;; basic event persistence
;;
;; directory/file(s)/event(s)/event
;; 
;; Usage:
;;   (require '[persi.core :as persi])
;;   (mp/new)    ;; start recording to a new file in the default
;;   (mp/pause)  ;; pause recording
;;   (mp/record) ;; restart recording
;;   (mp/save)   ;; save data to file
;;   (mp/open "my-saved-events.clj") ;; read events from saved file
;;   (count (mp/partition-by-timestamp)) ;; how many snippets do you have?
;;   (nth (mp/partition-by-timestamp) 2) ;; grab 3rd snippet
;; ======================================================================

;; ======================================================================
;; state atoms
(def persi-dir-name  (atom "persi_files"))
(def persi-file-name (atom nil))
(def persi-dirty     (atom false))
(def persi-events    (atom []))

;; ==================g====================================================
(defn- to-file
  "Save a clojure form to a file"
  [#^java.io.File file form]
  (with-open [w (java.io.FileWriter. file)]
    (print-dup form w)))

(defn- from-file
  "Load a clojure form from file."
  [#^java.io.File file]
  (with-open [r (java.io.PushbackReader. (java.io.FileReader. file))]
    (read r)))

(defn- now
  "Returns the current time in ms"
  []
  (System/currentTimeMillis))

(defn- get-timestamp-file-name
  ([] (str (.format (java.text.SimpleDateFormat. "yyMMdd_HHmmss") (now))
           ".clj")))

(defn- dirty! []
  (swap! persi-dirty (fn [x] true)))

(defn dirty? []
  @persi-dirty)

(defn- clean! []
  (swap! persi-dirty (fn [x] false)))

;; https://gist.github.com/1636017
(defn- wildcard-filter
  "Given a regex, return a FilenameFilter that matches."
  [re]
  (reify java.io.FilenameFilter
    (accept [_ dir name] (not (nil? (re-find re name))))))

(defn dir-list-longest-name-re
  "Given a directory and a regex, return a sorted seq of matching
  filenames. To find something like *.txt you would pass in
  \".*\\\\.txt\" "
  [dir re]
  (do
    (println "ppp-" dir "-" re )
    (.getName ;; -->
     (last
      (sort-by #(count (.getName %))
               (.list (clojure.java.io/file dir)
                      (wildcard-filter (java.util.regex.Pattern/compile re))))))))
  
(defn safe-old-dir-file
  [cur-dir-name]
FIXME -- here is where we go wrong... need to be more awake
  (let [last-dir-re (str cur-dir-name "*")
        cur-dir-file (java.io.File. cur-dir-name)
        cur-dir-parent (.getParent cur-dir-file)
        cur-dir-parent (if (nil? cur-dir-parent)
                         (java.io.File. ".")
                         cur-dir-parent)
        _     (swank.core/break)
        longest-dir-name (dir-list-longest-name-re cur-dir-parent last-dir-re)
        safe-dir-name (str longest-dir-name "_o")]
    (swank.core/break)
    (if (not safe-dir-name)
      (.rnameTo (java.io.File. safe-dir-name)))))

(defn- init-dir!
  [dir-name keep-cur-dir]
  (let [dir-file (java.io.File. dir-name)
        dir-exists (and (.exists dir-file)  ;; FIXME - what if someone names a file the same as the directory?
                        (.isDirectory dir-file))]
    ;; keep-cur dir-exists
    ;;   0         0        create new directory
    ;;   0         1        rename old dir, create new dir
    ;;   1         0        assert
    ;;   1         1        no-op 
    (assert (not (and keep-cur-dir (not dir-exists))))
    (when (not keep-cur-dir)
      (if dir-exists
        ;; rename old directory
        (.java.io.File.renameTo (safe-old-dir-file dir-name)))
      ;; create new dir
      (.mkdirs dir-file)
      ;;(.close dir-file) ;; ???
      (swap! persi-dir-name (fn [x] dir-name)))))

;; ======================================================================
;; public api fillows
(defn init!
  "Initialize the persi directory."
  ([] (init! @persi-dir-name true))
  ([dir-name keep-cur-dir]
     (init-dir! dir-name keep-cur-dir)
     (swap! persi-file-name (fn [x] nil)) ;; FIXME?
     (clean!)))
       
(defn new
  "Start a new midi persistence file, losing all prior events. Returns
  new file-name."  []
  (let [name (get-timestamp-file-name)]
    (swap! persi-file-name (fn [x] name))
    (swap! persi-events (fn [x] []))
    (clean!)
    ;;(record)
    name))

(defn save
  "Save the current events to a file (if necessary).  Returns true if it saved the file."
  []
  (if (and (not (nil? @persi-file-name))
           (dirty?))
    (do
      (to-file (java.io.File. @persi-file-name) @persi-events)
      (clean!)
      true)
    false))

(defn open
  "Open an existing file and read in the events"
  [file-name]
  (swap! persi-file-name (fn [x] file-name))
  (swap! persi-events (fn [x] (from-file (java.io.File. file-name))))
  (clean!)
  nil)

(defn add-event! [e]
  (swap! persi-events conj e)
  (dirty!))

(defn events
  "Return the event list"
  []
  @persi-events)

(defn get-file-name
  []
  (str @persi-dir-name "/" @persi-file-name)) ;; FIXME!

;; ======================================================================
;; manipulating events

(defn summary
  "summarize the current situation"
  []
  (println "persi-dirty:   " @persi-dirty)
  (println "persi-dir-name:" @persi-dir-name)
  (println "persi-file-name:" @persi-file-name)
  (println "event count:    " (count @persi-events)))

(comment
  (init! "test/persi-test-dir" false)

  )

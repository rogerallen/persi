;; ======================================================================
;; basic event persistence
(ns persi.core
  (:require [clojure.java.io :as cio]))

;; ======================================================================
;; state atoms
(def persi-default-dir-name "persi_files") 
(def persi-dir-name  (atom nil))
(def persi-file-name (atom nil))
(def persi-dirty     (atom false))
(def persi-events    (atom []))

;; ======================================================================
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

(defn- clean! []
  (swap! persi-dirty (fn [x] false)))

;; https://gist.github.com/1636017
(defn- wildcard-filter
  "Given a regex, return a FilenameFilter that matches."
  [re]
  (reify java.io.FilenameFilter
    (accept [_ dir name] (not (nil? (re-find re name))))))

(defn- dir-list-longest-name-re
  "Given a directory and a regex, return a sorted seq of matching
  filenames. To find something like *.txt you would pass in
  \".*\\\\.txt\" "
  [dir re]
  (last
   (sort-by #(count %)
            (.list (clojure.java.io/file dir)
                   (wildcard-filter (java.util.regex.Pattern/compile re))))))
;;(dir-list-longest-name-re "test" "persi-test-dir*")

;;(swank.core/break)
(defn- safe-old-dir-file
  [cur-dir-name]
  (let [;;_ (swank.core/break)
        cur-dir-file (java.io.File. cur-dir-name)
        cur-dir-filename (.getName cur-dir-file)
        last-dir-re (str cur-dir-filename "*") ;; don't include path to dir
        cur-dir-parent (.getParent cur-dir-file)
        cur-dir-parent (if (nil? cur-dir-parent)
                         "." ;(java.io.File. ".")
                         cur-dir-parent)
        longest-dir-name (dir-list-longest-name-re cur-dir-parent last-dir-re)
        ;; FIXME -- this probably isn't kosher
        safe-dir-name (str cur-dir-parent "/" longest-dir-name "_o")]
    (java.io.File. safe-dir-name)))

(defn- init-dir!
  [dir-name keep-cur-dir]
  (let [dir-file (java.io.File. dir-name)
        dir-exists (and (.exists dir-file)  ;; FIXME - what if someone names a file the same as the directory?
                        (.isDirectory dir-file))]
    ;; keep-cur dir-exists
    ;;   0         0        create new directory
    ;;   0         1        rename old dir, create new dir
    ;;   1         0        create new directory
    ;;   1         1        no-op 
    (when (or (not keep-cur-dir)
              (and keep-cur-dir (not dir-exists)))
      (if dir-exists
        ;; rename old directory
        (.renameTo dir-file (safe-old-dir-file dir-name)))
      ;; create new dir
      (.mkdirs dir-file)
      ;;(.close dir-file) ;; ???
      (swap! persi-dir-name (fn [x] dir-name)))))

;; ======================================================================
;; public api follows
(defn dirty? []
  @persi-dirty)

(defn init!
  "Initialize the persi directory."
  ([] (init! persi-default-dir-name true))
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
      (to-file (java.io.File. @persi-dir-name @persi-file-name) @persi-events)
      (clean!)
      true)
    false))

(defn open
  "Open an existing file and read in the events"
  [file-name]
  (swap! persi-file-name (fn [x] file-name))
  (swap! persi-events (fn [x] (from-file (java.io.File. @persi-dir-name file-name))))
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
  @persi-file-name)

(defn get-dir-name
  []
  @persi-dir-name)

(defn summary
  "summarize the current situation"
  []
  (println "persi-dirty:   " @persi-dirty)
  (println "persi-dir-name:" @persi-dir-name)
  (println "persi-file-name:" @persi-file-name)
  (println "event count:    " (count @persi-events)))

;; ======================================================================
(comment
  (init! "persi_testy" false)
  (persi.core/new)
  (persi.core/add-event! false)
  (events)
  (save)
  (persi.core/new)
  (persi.core/add-event! true)
  (save)
  (get-file-name)
  (summary)
  (init!)
  )

;; ======================================================================
;; basic persistence library
(ns persi.core
  ^{:author "Roger Allen"
    :doc "Basic list persistence.  Create a file holding a list of
    items, add items to the end of the list. See
    http://github.com/rogerallen/persi for documentation and usage."}
  (:require [clojure.java.io :as cio]))

;; ======================================================================
;; state atoms
(defonce persi-default-dir-name "persi_files") 
(defonce persi-dir-name  (atom nil))
(defonce persi-file-name (atom nil))
(defonce persi-dirty     (atom false))
(defonce persi-list      (atom []))
(defonce persi-map       (atom {}))

;; ======================================================================
(defn- to-file
  "Save a clojure form to a file.  Uses print-dup so the form is readable."
  [#^java.io.File file form]
  (with-open [w (java.io.FileWriter. file)]
    (print-dup form w)))

(defn- from-file
  "Load a clojure form from file.  Assumes it was written via to-file."
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
                         "."
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
;; PUBLIC API
(defn dirty?
  "are the data saved?"
  []
  @persi-dirty)

;; adding bang! to most of my API calls because I am not really
;; thinking of the multi-threaded case.  I'm designing this for one
;; user & one open file.
(defn init!
  "Initialize the persi directory. Set file name to nil."
  ([] (init! persi-default-dir-name true))
  ([dir-name keep-cur-dir]
     (init-dir! dir-name keep-cur-dir)
     (swap! persi-file-name (fn [x] nil))
     (clean!)
     nil))
       
(defn new!
  "Start a new midi persistence file, losing all prior data. If you
  do not pass in your own file name, it uses a file-name based on
  a timestamp (e.g. 130129_120000.clj).  Returns the filename."
  ([] (new! (get-timestamp-file-name)))
  ([name]
     (swap! persi-file-name (fn [x] name))
     (swap! persi-list (fn [x] []))
     (swap! persi-map (fn [x] {}))
     (clean!)
     name))

(defn save!
  "Save the current data to a file (if necessary).  Returns true if
  it saved the file.  Filename should be relative to the persi-dir-name."
  []
  (if (and (not (nil? @persi-file-name))
           (dirty?))
    (let [the-data [@persi-list @persi-map]]
      (to-file (java.io.File. @persi-dir-name @persi-file-name)
               the-data)
      (clean!)
      true)
    false))

(defn open!
  "Open an existing file and read in the data. Filename should be
  relative to the persi-dir-name"
  [file-name]
  (swap! persi-file-name (fn [x] file-name))
  (let [[the-list the-map] (from-file (java.io.File. @persi-dir-name file-name))]
    (swap! persi-list (fn [x] the-list))
    (swap! persi-map (fn [x] the-map)))
  (clean!)
  nil)

(defn append!
  "append e to the end of the persi-list"
  [e]
  (swap! persi-list conj e)
  (dirty!))

(defn insert!
  "add k and v to persi-map"
  [k v]
  (swap! persi-list assoc k v)
  (dirty!))

(defn get-list
  "Return the list"
  []
  @persi-list)

(defn get-map
  "Return the map"
  []
  @persi-map)

(defn get-file-name
  []
  @persi-file-name)

(defn get-dir-name
  []
  @persi-dir-name)

(defn summary
  "summarize the current situation"
  []
  (println "persi-dirty:    " (dirty?))
  (println "persi-dir-name: " (get-dir-name))
  (println "persi-file-name:" (get-file-name))
  (println "list count:     " (count @persi-list))
  (println "map count:      " (count @persi-map)))

;; ======================================================================
(comment
  (init! "persi_testy" false)
  (persi.core/new)
  (persi.core/append! false)
  (get-list)
  (save)
  (persi.core/new)
  (persi.core/append! true)
  (save)
  (get-file-name)
  (summary)
  (init!)
  )

;; ======================================================================
;; basic persistence library
(ns persi.persi
  ^{:author "Roger Allen"
    :doc "Basic list & dict persistence.  Create a file holding a list
    of items, add items to the end of the list. See
    http://github.com/rogerallen/persi for documentation and usage."}
  (:require [clojure.java.io :as cio]))

;; ======================================================================
;; internal state
;; This is limited-usage library.  Not thinking of the multi-threaded
;; case.  Designed for one user & one open file.
(def persi-default-dir-name "persi_files")
(defonce persi-state (atom {:dir-name  nil
                            :file-name nil
                            :dirty     false
                            :list      []
                            :map       {}}))

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
  (swap! persi-state assoc :dirty true))

(defn- clean! []
  (swap! persi-state assoc :dirty false))

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
            (.list (cio/file dir)
                   (wildcard-filter (java.util.regex.Pattern/compile re))))))

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
      (swap! persi-state assoc :dir-name dir-name))))

;; ======================================================================
;; PUBLIC API
(defn get-list
  "Return the list"
  []
  (:list @persi-state))

(defn get-map
  "Return the map"
  []
  (:map @persi-state))

(defn get-file-name
  "Return the current persi file name"
  []
  (:file-name @persi-state))

(defn get-dir-name
  "Return the current persi directory name. Files are stored relative to this."
  []
  (:dir-name @persi-state))

(defn dirty?
  "are the data saved?"
  []
  (:dirty @persi-state))

(defn init!
  "Initialize the persi directory. Set file name to nil."
  ([] (init! persi-default-dir-name true))
  ([dir-name keep-cur-dir]
     (init-dir! dir-name keep-cur-dir)
     (swap! persi-state assoc :file-name nil)
     (clean!)
     nil))

(defn new!
  "Start a new midi persistence file, losing all prior data. If you
  do not pass in your own file name, it uses a file-name based on
  a timestamp (e.g. 130129_120000.clj).  Returns the filename."
  ([] (new! (get-timestamp-file-name)))
  ([name]
     (swap! persi-state assoc :file-name name :list [] :map {})
     (clean!)
     name))

(defn save!
  "Save the current data to a file (if necessary).  Returns true if
  it saved the file.  Filename should be relative to the persi-dir-name."
  []
  (if (and (not (nil? (get-file-name)))
           (dirty?))
    (let [the-data [(get-list) (get-map)]]
      (to-file (java.io.File. (get-dir-name) (get-file-name))
               the-data)
      (clean!)
      true)
    false))

(defn open!
  "Open an existing file and read in the data. Filename should be
  relative to the persi-dir-name"
  [file-name]
  (let [[the-list the-map] (from-file (java.io.File. (get-dir-name) file-name))]
    (swap! persi-state assoc
           :file-name file-name
           :list the-list
           :map  the-map)
    (clean!)
    nil))

(defn append!
  "append e to the end of the persi-list"
  [e]
  (swap! persi-state (fn [x] (assoc x :list (conj (:list x) e))))
  (dirty!))

(defn set!
  "add k and v to persi-map"
  [k v]
  (swap! persi-state (fn [x] (assoc x :map (assoc (:map x) k v))))
  (dirty!))

(defn summary
  "summarize the current situation"
  []
  (println "dirty?:    " (dirty?))
  (println "dir-name:  " (get-dir-name))
  (println "file-name: " (get-file-name))
  (println "list count:" (count (get-list)))
  (println "map count: " (count (get-map))))

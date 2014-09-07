# persi

A Clojure library designed to save data to files in a directory.  I
use this to keep a simple record of midi keyboard events.  That use
case matches the limitations of the file format: one list and one
dictionary per file.  Not multi-thread safe.  One user, one
file open at a time.

[![Clojars Project](http://clojars.org/persi/latest-version.svg)](http://clojars.org/persi)

## Usage

```clj
(require [persi.persi :as persi])

;; only necessary if the main directory where you are saving files has
;; not been created yet
(persi/init!)

;; get a new file to write
(persi/new!)  ;; -> "130129_181920.clj"
;; append an event
(persi/append! {:note 60 :velocity 97})
;; use the map for other notes
(persi/insert! :comment "I would like to make note of this")
;; save the file
(persi/save!)

;; load an earlier file
(persi/open! "130128_180000.clj")
(persi/get-list) ;; -> [{:note 50 :velocity 90} {:note 48 :velocity 99}]
(persi/get-map)  ;; -> {:comment "An earlier comment"}
```

## API

* Adding data to the current persi file
  * *append!* [e]* - append e to the end of the list
  * *set! [k v]* - add k and v to the map
* Getter information about the current persi file
  * *get-list []*
  * *get-map []*
  * *get-file-name []*
  * *get-dir-name []*
  * *dirty? []*
  * *summary []* - summarize the current situation.
* Initialization of the file
  * *init! [] or [dir-name keep-cur-dir]* - Initialize the persi directory. Set file name to nil.
  * *new! [] or [name]* - Start a new midi persistence file, losing all prior data. If you
   do not pass in your own file name, it uses a file-name based on
   a timestamp (e.g. 130129_120000.clj).  Returns the filename.
  * *save! []* - Save the current data to a file (if necessary).  Returns true if
   it saved the file.  Filename should be relative to the persi-dir-name.
  * *open! [file-name]* - Open an existing file and read in the data. Filename should be
   relative to the persi-dir-name.

## License

Copyright © 2013-2014 Roger Allen (rallen@gmail.com)

Distributed under the Eclipse Public License, the same as Clojure.

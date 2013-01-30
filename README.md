# persi

A Clojure library designed to save data to files in a directory.  I
want to use this to keep a simple record of midi keyboard events.  I
might be reinventing the wheel, but I also consider this a learning
exercise.

## Usage

```clj
(require [persi.core :as persi])

;; only if the main directory has not been created yet
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

## License

Copyright © 2013 Roger Allen (rallen@gmail.com)

Distributed under the Eclipse Public License, the same as Clojure.

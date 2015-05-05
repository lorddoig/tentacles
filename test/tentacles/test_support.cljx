(ns tentacles.test-support
  #+clj (:require [clojure.edn :as edn])
  #+clj (:import (java.io FileNotFoundException))
  #+cljs (:require-macros [tentacles.test-support :refer [get-test-data]]))

#+clj
(defmacro ^:also-cljs get-test-data []
  (let [path (or (System/getenv "DATA_PATH") "testinfo.edn")
        contents (try (slurp path)
                      (catch FileNotFoundException _
                        (throw (FileNotFoundException. (str "Test data file at " path " not found.  See the README for details.")))))]
    (edn/read-string contents)))

(def test-data (get-test-data))
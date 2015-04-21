(ns amnixiel.main
    (:require [clojure.java.io :as io]
              [clojure.xml :as xml]
              [amnixiel.parser :as parser]
              [amnixiel.emitter :as emitter]
              [clojure.zip :as zip]))
(defn open [fs]
    (map io/file fs))

(defn main [& [f]]
    (print (clojure.data.xml/indent-str (first
        (emitter/kml 
            (reduce #(concat %1
                (parser/parse-networks 
                (-> %2 
                  (xml/parse parser/startparse-sax) zip/xml-zip)))
             '() (open *command-line-args*)))))))

(ns amnixiel.test
    (:import (javax.xml.parsers SAXParser SAXParserFactory))
    (:require [clojure.java.io :as io]
              [clojure.xml :as xml]
              [amnixiel.colours :as colours]
              [amnixiel.parser :as parser]
              [clojure.zip :as zip])
    (:use
              [clojure.core.strint]
              [clojure.data.xml]
              [clojure.data.zip.xml]))

(defn strip-meta 
    "Strip the initial <?xml enocding=blah?> from an xml string.
     It does not look like the Clojure data.xml library provides an alternative
     for not printing this metadata."
     [xml-str]
    (clojure.string/replace xml-str #"<\?[^>]+\?>" ""))

(defn or-nil [f]
    (try
        (f)
         (catch Exception e nil)))

(defn parse-ssid-block [m]
    (or-nil
    #(into {}
        (let [ssid (xml1-> m :SSID)]
                {:essid (text (xml1-> ssid :essid))
                 :encryption (map text (xml-> ssid :encryption))}))))

(defn parse-gps-block [m]
    (or-nil 
    #(into {}
        (let [gps-info (xml1-> m :gps-info)]
            {:lon (text (xml1-> gps-info :max-lon))
             :lat (text (xml1-> gps-info :max-lat))}))))
            
(defn parse-network-block [m]
    (when-let [gps (parse-gps-block m)]
        (when-let [ssid (parse-ssid-block m)]
            (merge {
                 ; :first-time (attr m :first-time)
                 ; :last-time (attr m :last-time) 
                  :bssid (text (xml1-> m :BSSID))
                  :channel (text (xml1-> m :channel))} 
                 ssid
                 gps))))
    
(def test-xml (element :foo {:foo-attr "foo value"}
                     (element :bar {:bar-attr "bar value"}
                       (element :baz {} "The baz value1")
                       (element :baz {} "The baz value2")
                       (element :baz {} "The baz value3"))))

(defn mkdesc-content [n]
    (element :div {}
        (element :p {:style "font-size:8pt;font-family:monospace;"} 
            (<< "(~(n :lon),~(n :lat))"))
        (element :ul {}
            (element :li {} (<< "BSSID : ~(n :bssid)"))
            (element :li {} (<< "Channel : ~(n :channel)"))
            (element :li {} (<< "Encrypt : ~(clojure.string/join \\space (n :encryption))")))))
           
(defn mkdesc [n]
    (sexp-as-element [:description {} 
        [:-cdata (strip-meta (emit-str (mkdesc-content n)))]]))

(defn network->kml [n]
    (element :Placemark {}
        (mkdesc n)
        (element :name {} (n :essid))
        (element :Point {}
            (element :extrude {} "1")
            (element :alitiudeMode {} "relativeToGround")
            (element :coordinates {} (<< "~(n :lon),~(n :lat),0")))
        (element :styleUrl {} 
            (<< "#~(colours/colour->id (colours/pick-colour (n :encryption)))"))))

;(print (main "test.xml"))

;(print (indent-str (first (main "test.xml"))))

(defn style->kml [c]
    (element :style {:id (colours/colour->id c)}
        (element :LabelStyle {}
            (element :color {} (colours/colour->hex c)))))

(defn kml [n]
    (def root (zip/xml-zip (element :Document {}
            (style->kml :red)
            (style->kml :orange)
            (style->kml :green)
            (style->kml :black))))
    (defn app [e] (zip/append-child root e))
        (reduce #(
           zip/append-child %1 (network->kml %2)) root (remove nil? n)))

(defn parse-networks [root]
          (map #(parse-network-block %) (xml-> root :wireless-network)))

(defn main [f]
    (def root (-> f io/resource io/file 
               (xml/parse parser/startparse-sax) zip/xml-zip))
    (print (indent-str (first (kml (parse-networks root))))))
    ;(parse-networks root))

;(print (indent-str (main "test-mini.xml")))
;(print (first (main "test.xml")))
;(print (main "test.xml"))
;(print (first (main "test.xml")))
;(print "\n>>\n\n")
;(print (indent-str (first (main "test.xml"))))
;(println "\n")

;(def test-xml (element :foo {:foo-attr "foo value"}
;                     (element :bar {:bar-attr "bar value"}
;                       (element :baz {} "The baz value1")
;                       (element :baz {} "The baz value2")
;                       (element :baz {} "The baz value3"))))

;(print (strip-meta (indent-str (mkdesc))))


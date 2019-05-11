(ns commcare-cli.helpers
  (:require [clojure.string :as string]))

(defn long-str [& strings] (string/join "\n" strings))

(defn clear-view []
  (doall
    (map (fn [x] (println " ")) (range 5))))

;; String Integer -> Boolean
(defn validate-number-input [user-input max-number]
  (try
    (let [i (Integer/parseInt user-input)]
      (if (or (< i 1) (> i max-number))
        (do
          (println "Enter a number between 1 and " max-number)
          -1)
        (- i 1)))
    (catch NumberFormatException e
      (do
        (println "Enter a number between 1 and " max-number)
        -1))))

(defn ppxml [xml]
  (let [in (javax.xml.transform.stream.StreamSource.
             (java.io.StringReader. xml))
        writer (java.io.StringWriter.)
        out (javax.xml.transform.stream.StreamResult. writer)
        transformer (.newTransformer
                      (javax.xml.transform.TransformerFactory/newInstance))]
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/INDENT "yes")
    ;; is the following needed?
    (.setOutputProperty transformer
                        "{http://xml.apache.org/xslt}indent-amount" "2")
    (.setOutputProperty transformer
                        javax.xml.transform.OutputKeys/METHOD "xml")
    (.transform transformer in out)
    (println (-> out .getWriter .toString))))

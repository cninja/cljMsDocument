(ns cljmsdoc.util
  (:require [cljmsdoc.sql :as sql]
            [cljmsdoc.queries :as qs])
  )

(defn set-clipboard-text! [t]
  (let [ss (java.awt.datatransfer.StringSelection. (str t))]
    (.setContents (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)) ss nil)))



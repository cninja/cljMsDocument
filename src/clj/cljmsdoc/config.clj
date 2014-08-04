(ns cljmsdoc.config
  (:require [clojure.edn :as edn ]
            [clojure.java.io :as io]
            [clojure.pprint :as p]))

(def config-dir (str (System/getProperty "user.home") "/.cljMsDocument/"))
(def dbs (atom (try (clojure.edn/read-string (slurp (str config-dir "connections")))
                 (catch Exception e {}))))
(defn save-dbs []
  (do
    (.mkdir (io/file config-dir ))
    (spit (str config-dir "connections")
          (with-out-str (p/pprint (reduce #(assoc %1 (first %2) (dissoc (second %2) :password)) {} @dbs)) ) )))

(defn remove-db [db-name]
  (reset! dbs (dissoc @dbs db-name)))
(defn add-db [name subprotocol subname databaseName user password]
  (do
    (reset! dbs (assoc @dbs name {:classname (condp = subprotocol
                                            "sqlserver" "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                                            (throw (Exception. "Unknown subprotocol")))
                               :name name
                               :subprotocol subprotocol
                               :subname subname
                               :databaseName databaseName
                               :user user
                               :p password})))
  (save-dbs))




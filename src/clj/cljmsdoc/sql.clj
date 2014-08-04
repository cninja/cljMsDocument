(ns cljmsdoc.sql
  (:require [clojure.java.jdbc :as sql]))

(def
  ^{ :doc "The current database conenction settings. Not thread safe if accessing multiple dbs"}
  db (atom nil))

(defn set-db [new-db]
  (reset! db new-db))
(defn get-db []
  @db)

(defn as-vector [obj]
  (if (vector? obj)
    obj
    (vector obj)))
(defn execute! [sql & extra]
  (if (nil? @db)
    (throw (Exception. "Call set-db first"))
    (apply sql/execute! @db (as-vector sql) extra)))
(defn rows [sql & extra ]
  "Return all the rows retreived for the sql query as a vector"
  (if (nil? @db)
    (throw (Exception. "Call set-db first"))
    (apply sql/query @db (as-vector sql) :identifiers identity extra)))
(defn row
  "Return the first row retreived for the sql query"
  [sql & extra]
  (let [as-arrays? (:as-arrays? (apply array-map extra))]
    ((if as-arrays? second first) (apply rows sql extra))))

(defn scalar
  "Return the first element of the first row for the sql query"
  [sql]
  (first (row sql :as-arrays? true)))

(defn col
  "Return a vector of the first element from every retreived row"
  [sql]
  (map first (next (rows sql :as-arrays? true))))

(defn hash-map
  "Return a map from a query. For each row, the first element is the key and the second is the value"
  [sql]
  (reduce #(assoc %1 (first %2) (second %2)) {} (next (rows sql :as-arrays? true))))


(defn hash-map-array
  "Return a map from a query. The first element is the key and the value of the map for that key is an array of second values"
  [sql]
  (reduce #(assoc %1 (first %2) (conj (vec (get %1 (first %2))) (second %2))) {} (next (rows sql :as-arrays? true))))





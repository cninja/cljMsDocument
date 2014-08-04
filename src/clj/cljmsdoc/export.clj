(ns cljmsdoc.export
  (:require [cljmsdoc.queries :as q]
            )
  (:use hiccup.page))

(defn defunct?
  [x]
  (let [name (or (:name x) x)]
    (or (.contains name "DEFUNCT")
         (.contains name "DEFUCNT"))))
(defn field-export
  [column-types column-references column-sources column-descriptions schematablecol]
  (let [[schema table col] (.split schematablecol "\\.")
        schematable (str schema "." table)
        col-type (get column-types schematablecol)
        li-information (remove nil?
                               (concat
                                (if-let [description (get column-descriptions col)] [[:li description]])
                                 (if (.contains col-type "PK")
                                   (let [references (get column-references schematablecol)]
                                     (if (not (zero? (count references)))
                                       (vec (map #(vector :li (str "Used by " %)) references)))))
                                 (if (.contains col-type "FK")
                                   (let [source (get column-sources schematablecol)]
                                     [[:li (str "References " source)]]))))
        ]
    (vec (concat [:li.field col " - " col-type]
                 (if (not= 0 (count li-information)) [[:ul.item-information li-information]])
                 ))))

(defn table-export
  [table-descriptions columns column-types column-references column-sources schematable]
  (let [[schema table] (.split schematable "\\.")
        my-columns (get columns schematable)
        column-descriptions (q/all-column-descriptions schema table)]
    [[:h2.table table]
     [:p.description (get table-descriptions table)]
     [:ul (map (partial field-export column-types column-references column-sources column-descriptions)
               (remove defunct? my-columns))]]))
(defn schema-export
  [schema-descriptions tables columns column-types column-references column-sources schema-val]
  (let [schema (:value schema-val)
        tables (remove defunct? (get tables schema))
        table-descriptions (q/all-table-descriptions schema)]
    (if (zero? (count tables))
      []
      (apply concat [[:h1.schema (str "Schema: " schema)]
                     [:p.description  (get schema-descriptions schema)]]
             (map (partial table-export table-descriptions columns column-types column-references column-sources) tables)))))
;(schema-export identity {:value "Catalog"})
;(field-export {:value "Catalog.Item.Id"})
;(q/column-references "Catalog.Item.Id")
;(q/column-source "Catalog.Item.Id")
;(q/get-description "Catalog" "Item" "Id")

;(html5 (schema-export {:value "Catalog"}))
(defn export-style
  []
  (str "h1 { font-family: Calibri; font-size: 14pt; font-weight: 700; color: rgb(46,116,181) }\n"
       "h2 { font-family: Calibri; font-size: 13pt; font-weight: normal; color: rgb(46, 116, 181) }\n"
       "p,li { font-family: Calibri; font-size: 11pt; font-weight: normal; color: black }\n"
       ))
(defn db-export
  []
  (let [columns (q/all-columns)
        tables (q/all-tables)
        column-types (q/all-column-type-decr)
        column-references (q/all-column-references)
        column-sources (q/all-column-source)
        schema-descriptions (q/all-schema-descriptions)
        ]
    (html5
     [:head [:style {:type "text/css"} (export-style)]]
     [:body
      (apply concat [[:h1 "Overview"]
                     [:p.description (q/get-description nil nil nil)]]
             (map (partial schema-export schema-descriptions tables columns column-types column-references column-sources) (q/schemas)))])))




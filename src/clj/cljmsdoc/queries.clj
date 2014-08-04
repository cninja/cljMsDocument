(ns cljmsdoc.queries
  (:require [cljmsdoc.sql :as sql]
            [clojure.string :as s]))

(defn set-db
  [db]
  (sql/set-db db))

(defn get-db
  []
  (sql/get-db))

(defn databases
  []
  (sql/rows "SELECT name, name AS value
            FROM sys.sysdatabases
            ORDER BY name"))
(defn schemas
  []
  (sql/rows "SELECT DISTINCT TABLE_SCHEMA AS name, TABLE_SCHEMA AS value
   FROM INFORMATION_SCHEMA.TABLES
   ORDER BY TABLE_SCHEMA"))

(defn tables
  [schema]
  (sql/rows ["SELECT DISTINCT TABLE_NAME AS name, TABLE_SCHEMA + '.' + TABLE_NAME AS value
             FROM INFORMATION_SCHEMA.TABLES
             WHERE TABLE_SCHEMA = ?
             ORDER BY TABLE_NAME" schema]))
(defn all-tables
  []
  (sql/hash-map-array ["SELECT DISTINCT TABLE_SCHEMA, TABLE_SCHEMA + '.' + TABLE_NAME AS value
             FROM INFORMATION_SCHEMA.TABLES
             ORDER BY value" ]))
(defn columns
  [schematable]
  (let [[schema table] (.split schematable "\\.")]
    (sql/rows ["SELECT COLUMN_NAME AS name, TABLE_SCHEMA + '.' + TABLE_NAME + '.' + COLUMN_NAME AS value
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION" schema table])))
(defn all-columns
  []
    (sql/hash-map-array ["SELECT TABLE_SCHEMA + '.' + TABLE_NAME, TABLE_SCHEMA + '.' + TABLE_NAME + '.' + COLUMN_NAME AS value
                FROM INFORMATION_SCHEMA.COLUMNS
                ORDER BY ORDINAL_POSITION" ]))
(defn column-type-decr
  [schematablecol]
  (let [[schema table col] (.split schematablecol "\\.")
        schematable (str schema "." table)]
    (sql/scalar ["SELECT
                       CASE WHEN tc.CONSTRAINT_TYPE = 'PRIMARY KEY' THEN 'PK '
                           WHEN tc.CONSTRAINT_TYPE = 'FOREIGN KEY' THEN 'FK '
                           ELSE '' END +
                       CASE WHEN c.IS_NULLABLE = 'NO' THEN '' ELSE 'nullable ' END +
                       DATA_TYPE +
                       ISNULL('(' + CASE WHEN c.DATA_TYPE = 'ntext' THEN '' ELSE CAST(c.CHARACTER_MAXIMUM_LENGTH AS varchar) END + ')', '') AS Type
                FROM INFORMATION_SCHEMA.COLUMNS c
                LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku
                  ON ku.TABLE_SCHEMA = c.TABLE_SCHEMA AND ku.TABLE_NAME = c.TABLE_NAME AND ku.COLUMN_NAME = c.COLUMN_NAME
                LEFT JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                  ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
                WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ? AND c.COLUMN_NAME = ?
                ORDER BY tc.CONSTRAINT_TYPE DESC
                " schema table col])))
(defn all-column-type-decr
  []
   (sql/hash-map ["SELECT
                       c.TABLE_SCHEMA + '.' + c.TABLE_NAME + '.' + c.COLUMN_NAME,
                       CASE WHEN tc.CONSTRAINT_TYPE = 'PRIMARY KEY' THEN 'PK '
                           WHEN tc.CONSTRAINT_TYPE = 'FOREIGN KEY' THEN 'FK '
                           ELSE '' END +
                       CASE WHEN c.IS_NULLABLE = 'NO' THEN '' ELSE 'nullable ' END +
                       DATA_TYPE +
                       ISNULL('(' + CASE WHEN c.DATA_TYPE = 'ntext' THEN '' ELSE CAST(c.CHARACTER_MAXIMUM_LENGTH AS varchar) END + ')', '') AS Type
                FROM INFORMATION_SCHEMA.COLUMNS c
                LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku
                  ON ku.TABLE_SCHEMA = c.TABLE_SCHEMA AND ku.TABLE_NAME = c.TABLE_NAME AND ku.COLUMN_NAME = c.COLUMN_NAME
                LEFT JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                  ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
                "]))
(defn column-references
  [schematablecol]
  (let [[schema table col] (.split schematablecol "\\.")
        schematable (str schema "." table)]
    (sql/col ["
                 SELECT ku_fk.TABLE_SCHEMA + '.' + ku_fk.TABLE_NAME + '.' + ku_fk.COLUMN_NAME AS Name
                  FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rf
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_pk
                    ON rf.UNIQUE_CONSTRAINT_NAME = ku_pk.CONSTRAINT_NAME
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_fk
                    ON rf.CONSTRAINT_NAME = ku_fk.CONSTRAINT_NAME
                  WHERE ku_pk.TABLE_SCHEMA = ? AND ku_pk.TABLE_NAME = ? AND ku_pk.COLUMN_NAME = ?
                  ORDER BY Name
                " schema table col])))
(defn all-column-references
  []
    (sql/hash-map-array ["
                 SELECT ku_pk.TABLE_SCHEMA + '.' + ku_pk.TABLE_NAME + '.' + ku_pk.COLUMN_NAME,
                        ku_fk.TABLE_SCHEMA + '.' + ku_fk.TABLE_NAME + '.' + ku_fk.COLUMN_NAME AS Name
                  FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rf
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_pk
                    ON rf.UNIQUE_CONSTRAINT_NAME = ku_pk.CONSTRAINT_NAME
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_fk
                    ON rf.CONSTRAINT_NAME = ku_fk.CONSTRAINT_NAME
                  ORDER BY Name
                "]))
(defn column-source
  [schematablecol]
  (let [[schema table col] (.split schematablecol "\\.")
        schematable (str schema "." table)]
    (sql/scalar ["
                 SELECT ku_pk.TABLE_SCHEMA + '.' + ku_pk.TABLE_NAME + '.' + ku_pk.COLUMN_NAME AS Name
                  FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rf
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_pk
                    ON rf.UNIQUE_CONSTRAINT_NAME = ku_pk.CONSTRAINT_NAME
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_fk
                    ON rf.CONSTRAINT_NAME = ku_fk.CONSTRAINT_NAME
                  WHERE ku_fk.TABLE_SCHEMA = ? AND ku_fk.TABLE_NAME = ? AND ku_fk.COLUMN_NAME = ?
                " schema table col])))
(defn all-column-source
  []
  (sql/hash-map ["
                 SELECT ku_fk.TABLE_SCHEMA + '.' + ku_fk.TABLE_NAME + '.' + ku_fk.COLUMN_NAME,
                 ku_pk.TABLE_SCHEMA + '.' + ku_pk.TABLE_NAME + '.' + ku_pk.COLUMN_NAME AS Name
                  FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rf
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_pk
                    ON rf.UNIQUE_CONSTRAINT_NAME = ku_pk.CONSTRAINT_NAME
                  JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku_fk
                    ON rf.CONSTRAINT_NAME = ku_fk.CONSTRAINT_NAME
                "]))

(defn get-description
  [schema table column]
  (let [prop "MS_Description"]
    (condp = nil
      schema (sql/scalar ["SELECT CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, default, default, default, default, default, default)" prop])
      table (sql/scalar ["SELECT CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, 'SCHEMA', ?, default, default, default, default)" prop schema])
      column  (sql/scalar ["SELECT CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, 'SCHEMA', ?, 'TABLE', ?, default, default)" prop schema table])
      (sql/scalar ["SELECT CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, 'SCHEMA', ?, 'TABLE', ?, 'COLUMN', ?)" prop schema table column])
      )))
(defn all-column-descriptions
  [schema table]
  (let [prop "MS_Description"]
      (sql/hash-map ["SELECT objname, CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, 'SCHEMA', ?, 'TABLE', ?, 'COLUMN', null)" prop schema table])
      ))
(defn all-table-descriptions
  [schema ]
  (let [prop "MS_Description"]
      (sql/hash-map ["SELECT objname, CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, 'SCHEMA', ?, 'TABLE', null, default, default)" prop schema ])
      ))
(defn all-schema-descriptions
  []
  (let [prop "MS_Description"]
      (sql/hash-map ["SELECT objname, CAST(value AS varchar(max)) FROM fn_listextendedproperty(?, 'SCHEMA', null, default, default, default, default)" prop ])
      ))
(defn set-description
  [desc schema table column]
  (let [prop "MS_Description"
        sp (if (get-description schema table column) "sys.sp_updateextendedproperty" "sys.sp_addextendedproperty")]
    (condp = nil
      schema (sql/execute! [(str "EXEC " sp " @name = ?, @value = ?
                                   ") prop desc])
      table  (sql/execute! [(str "EXEC " sp " @name = ?, @value = ?,
                                 @level0type = 'SCHEMA', @level0name = ?
                                   ") prop desc schema ])
      column (sql/execute! [(str "EXEC " sp " @name = ?, @value = ?,
                                 @level0type = 'SCHEMA', @level0name = ?,
                                 @level1type = 'TABLE', @level1name = ?
                                   ") prop desc schema table ])
      (sql/execute! [(str "EXEC " sp " @name = ?, @value = ?,
                                 @level0type = 'SCHEMA', @level0name = ?,
                                 @level1type = 'TABLE', @level1name = ?,
                                 @level2type = 'COLUMN', @level2name = ?
                                   ") prop desc schema table column])
      )))


(defn abbr-table
  [table-name q n]
  (let [shorten-fn #(.toLowerCase (apply str (re-seq #"[A-Z0-9]" %)))
        re-find-word #(re-find (re-pattern (str "\\b" %1 "\\b")) %2)
        just-table-n (shorten-fn (last (.split table-name "\\.") ))
        table-n (str (shorten-fn table-name) (if (= 1 n) "" n))
        ]
    (if (re-find-word just-table-n q)
      (if (re-find-word table-n q)
        (recur table-name q (inc n))
        table-n)
      just-table-n)))

(defn strip-subqueries
  [q]
  {:query q :stuff nil})
(defn unstrip-subqueries
  [q stripped-q]
  q)

(defn split-at-re [q re]
  (if-let [matches (re-find re q)]
    [(nth matches 1) (nth matches 2)]
    [q ""]))
(defn split-from [q] (split-at-re q #"(?is)(.*?)(\b(FROM|LEFT|RIGHT|JOIN|WHERE|GROUP BY|ORDER BY|HAVING)\b.*)"))
(defn split-where [q] (split-at-re q #"(?is)(.*?)(\b(WHERE|GROUP BY|ORDER BY|HAVING)\b.*)"))
(defn split-group-by [q] (split-at-re q #"(?is)(.*?)(\b(GROUP BY|ORDER BY|HAVING)\b.*)" ))

(defn split-query
  [q]
  (let [stripped-q (strip-subqueries q)
        [selectfromq whereq] (split-where (:query stripped-q))
        [selectq fromq] (split-from selectfromq)]
   (map #(unstrip-subqueries % stripped-q) [selectq fromq whereq])))
(defn list-columns
  [table-name table-n]
  (s/join ",\n       " (map #(str table-n "." (:name %)) (columns table-name))))
(defn new-table-query
  [table-name]
   (let [table-n (abbr-table table-name "" 1)]
     (str "SELECT " (list-columns table-name table-n) "\nFROM " table-name " AS " table-n "\n")))
(defn join-table-to-query
  [full-field-name query-on q]
  (let [i (.lastIndexOf full-field-name ".")
        table-name (.substring full-field-name 0 i)
        table-field (.substring full-field-name i)]
    (let [table-n (abbr-table table-name q 1)]
      (if (.equals (or q "") "")
        (new-table-query table-name)
        (let [[selectq fromq whereq] (split-query q)]
          (str (s/trim selectq) ",\n" (list-columns table-name table-n) "\n" (s/trim fromq)
               "\nJOIN " table-name " AS " table-n
               "\n  ON " table-n table-field " = " query-on
               "\n" (s/trim whereq)))))))

(defn limit-query
  [amt q]
  (if-let [matches (re-find #"(?s)(^\s*SELECT\s*)(TOP\s*\d+\s*)?(.*)" q)]
    (str (nth matches 1) (if (= amt 0) "" (str "TOP " amt " ")) (nth matches 3))
    q))

(defn quot
  [v]
  (cond (nil? v) "NULL"
        (instance? Boolean v) (if v "1" "0")
        (instance? java.util.Date v) (str "'" (.format (java.text.SimpleDateFormat. "yyyy-MM-dd  HH:mm:ss") v) "'")
        (string? v) (str "'" (s/replace v #"'" "''") "'")
        :else (.toString v)))
(defn add-where
  [q fieldname value]
  (let [stripped-q (strip-subqueries q)
        [selectfrom-q where-q] (split-where (:query stripped-q))
        [where-q groupby-q] (split-group-by where-q)
        extra-where (str (if (= where-q "") "\nWHERE " "\n  AND ")
                         fieldname
                         (if (nil? value) " IS " " = ")
                         (quot value))]
   (apply str (map #(unstrip-subqueries % stripped-q) [selectfrom-q where-q extra-where groupby-q]))))


(defn extract-fields
  [q]
  (let [[select-q from-q] (split-from (:query (strip-subqueries q)))
        select-q (limit-query 0 select-q)]
    select-q))


;(defn print-columns [schematable]
;  (apply str (map #(str  (first %) " - " (second %) "\r\n") (next (columns-decr schematable)))))
;(defn print-table [schematable]
;  (str "\r\n\r\nTable: " schematable "\r\n\r\n" (print-columns schematable)))
;(defn print-schema [schema]
;  (apply str "\r\nSchema: " (:name schema) "\r\n" (map print-table (filter #(not (.contains % "DEFU")) (map #(str (:name schema) "." (:name %)) (tables (:name schema)))))))
;(print (map print-schema (schemas)))

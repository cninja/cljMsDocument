(ns cljmsdoc.core
  (:require [cljmsdoc.queries :as q]
            [cljmsdoc.util :as u]
            [cljmsdoc.sql :as s]
            [cljmsdoc.config :as c]
            [cljmsdoc.export :as e]
            [seesaw.mouse]
            )
  (:use seesaw.core
        seesaw.mig
        seesaw.chooser
        seesaw.tree
        seesaw.dev))
(import '(javax.swing DefaultListCellRenderer)
        '(javax.swing.table DefaultTableCellRenderer))
(def root-atoms (atom []))
(defn add-root! [r] (reset! root-atoms (conj @root-atoms r)))
(defn by-id [id] (first (remove nil? (map #(select % [id]) @root-atoms))))
(defn rm-root! [id] (reset! root-atoms (remove #(= (by-id id) %) @root-atoms)))
(defn init-list-cell-renderer [renderer-fn]
  (proxy [DefaultListCellRenderer] []
			   (getListCellRendererComponent
			    [list item index isSelected? cellHasFocus?]
			    (proxy-super getListCellRendererComponent list (renderer-fn item) index isSelected? cellHasFocus?))))
(defn listen-id
  [c k func]
    (listen (if (keyword? c) (by-id c) c) k #(func %)))
(defmacro config-id!
  [id & code]
    `(if-let [c# (by-id ~id)]
       (config! c# ~@code)
       (throw (Exception. (str "Could not find " ~id)))))
(defmacro config-id
  [id & code]
    `(if-let [c# (by-id ~id)]
       (config c# ~@code)
       (throw (Exception. (str "Could not find " ~id)))))
(defmacro do-or-status
  [ok-text & code ]
  `(try
     ~@code
     (config-id! :#status-bar :text ~ok-text)
     (catch Exception f# (config-id! :#status-bar :text (.getMessage f#)))))
(defn user-data-id [id]
  (config-id id :user-data))
(defn get-name [e]
  (if (map? e)
    (:name e)
    (if (coll? e)
      (recur (first e))
      e)))
(def name-renderer (init-list-cell-renderer #(get-name %)))
(def db-item-types
  {:database "Database"
   :schema "Schema"
   :table "Table"
   :field "Field"})

(defn render-tree-item
  [renderer {:keys [value]}]
  (config! renderer :text (:name value)))
(defn is-branch? [n]
  (not= (:type n) (:field db-item-types)))
(def tree-get-children-memo
  (memoize (fn [my-type my-value]
             (let [set-type (fn [t items] (map #(assoc % :type (get db-item-types t)) items))]
              (condp = my-type
               (:database db-item-types) (set-type :schema (q/schemas))
               (:schema db-item-types) (set-type :table (q/tables my-value))
               (:table db-item-types) (set-type :field (q/columns my-value))
               (throw (Exception. (str "Unknown type" my-type))))))))
(defn tree-get-children [n]
  (tree-get-children-memo (:type n) (:value n)))
(defn set-item-label
  [str1 str2 user-d]
  (let [lbl-item (by-id :#lbl-item)]
    (config! lbl-item :text (str str1 ": " str2))
    (config! lbl-item :user-data user-d)))
(defn load-text
  [addr]
  (config-id! :#txt-documentation-area :text (apply q/get-description addr)))

(defn tree-item-selected [e]
  (do-or-status
   (str "Loaded description")
   (let [item-ancestry (vec (map #(:name %) (next (selection e))))
         item (last (selection e))
         desc-address [(first item-ancestry) (second item-ancestry) (get item-ancestry 2)]
         txt (apply q/get-description desc-address)
         ]
     (load-text desc-address)
     (set-item-label (:type item) (:value item) desc-address)
     )
   ))
(defn build-item-tree [db-name]
  (let [root {:type (:database db-item-types)
              :name db-name
              :value db-name}]
    (tree :root-visible? true
          :renderer render-tree-item
          :listen [:selection (fn [e] (tree-item-selected e))]
          :model (simple-tree-model is-branch? tree-get-children root))))
(defn btn-export-click [e]
  (do-or-status
    "Export complete"
   (let [update-fn #(config-id! :#status-bar :text %)]
    ;(let [update-fn #(and (config-id! :#status-bar :text %) (.updateUI (by-id :#status-bar)))]
     (update-fn "Begining export...")
     (if-let [export-file (choose-file :type :save :filters [["HTML Files" ["html"]]]) ]
       (do
         (.mkdir (.getParentFile export-file))
         (spit export-file (e/db-export)))))))

(defn btn-save-doc-click [e]
  (do-or-status
   (str "Updated " (config-id :#lbl-item :text))
   (let [desc (config-id :#txt-documentation-area :text)
         addr (user-data-id :#lbl-item)]
     (apply q/set-description desc addr))))

(defn populate-item-tree [db-name]
  (let [panel (by-id :#item-tree-container)]

    (.removeAll panel)
    (config! panel :center (scrollable (build-item-tree db-name)))
    (.updateUI panel)))

(defn get-dbs [none-text]
  (concat [none-text] (q/databases)))

(defn database-select-changed [new-val]
  (let [desc-address [nil nil nil]
        db-name (:value new-val)]
    (s/set-db (assoc (s/get-db) :databaseName db-name))
    (set-item-label "Database" db-name [nil nil nil])
    (load-text desc-address)
    (populate-item-tree db-name)))


(defn new-main-panel [f]
  (let [database-select (combobox :model (get-dbs "--Select Database--") :id :db-select :renderer name-renderer
                                  :listen [:selection (fn [e] (database-select-changed (selection e)))])
        status-bar (label :id :status-bar :text " foo ")
        item-tree-container (border-panel :id :item-tree-container :center (label :text "Select a database"))
        item-label (label :id :lbl-item :text "")
        txt-documentation-area (text :id :txt-documentation-area
                                     :wrap-lines? true
                                     :multi-line? true
                                     :font "MONOSPACED-PLAIN-12")
        btn-export (button :id :btn-export :text "Export" :listen [:action #(btn-export-click %)])
        btn-save-doc (button :id :btn-save-doc :text "Save" :listen [:action #(btn-save-doc-click %)])
        ]

    (border-panel
        :center (left-right-split
                 (border-panel
                  :north database-select
                  :center item-tree-container
                  :south (border-panel
                          :west (horizontal-panel :items [btn-export])))
                 (border-panel
                  :north item-label
                  :center txt-documentation-area
                  :south (border-panel
                          :east (horizontal-panel :items [btn-save-doc])))
                 :divider-location 1/3)
        :south status-bar)))



;Test
;(config! f :content (new-main-panel f))





(defn btn-db-connect-click [f]
  (do-or-status
   "Db Connected"
   (let [get-val #(text (by-id %))
         db {:classname (condp = (get-val :#db-type)
                                     "sqlserver" "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                                     (throw (Exception. "Unknown subprotocol")))
             :subprotocol (get-val :#db-type)
             :subname (str (get-val :#db-host) (if (selection (by-id :#cb-windowsauth)) ";integratedSecurity=true" ""))
             ;:databaseName (get-val :#db-databaseName)
             :user (get-val :#db-username)
             :password (get-val :#db-password)
            }]
     (q/set-db db)
     (q/schemas);Run SQL statement to verify that the db settings are correct
     (config! f :content (new-main-panel f))
     )))

(defn new-db-connect-panel [f]
  (let [status-bar (label :id :status-bar :text " Status Bar ")
        items  [
                ;["New/Edit"] [(combobox :model (get-dbs "New Db") :id :db-select :renderer name-renderer) "wrap"]
                ["DB Type"]  [(combobox :model ["sqlserver"] :id :db-type) "wrap"]
                ["Host"] [(text :columns 20 :id :db-host :text "//localhost:1433")]["ie: //localhost:1433" "wrap"]
                ["Windows Authentication"] [(checkbox :id :cb-windowsauth)]["If this is checked, username and password can be blank" "wrap"]
                ["Username"] [(text :columns 20 :id :db-username) "wrap"]
                ["Password"] [(password :columns 20 :id :db-password) "wrap"]
                ;["Database Name"] [(text :columns 20 :id :db-databaseName) "wrap"]
                ;[" "] [(checkbox :id :db-save-password :text "Store Password") "wrap"]
                ;[(button :id :build-db-save :text "Save")]
                ;[(button :id :build-db-remove :text "Delete Db")]
                [(button :id :btn-db-connect :text "Connect" :listen [:action (fn [e] (btn-db-connect-click f))])]
                ]
        ]
    ;(add-action-fn :#build-db-save db-save)
    ;(add-action-fn :#build-db-remove db-remove)
    ;(show! f)
    (border-panel :center (mig-panel :id :db-connect-panel :items items )
                  :south status-bar)))

(def f (frame :id :main-frame :title "Documentation Tool" :width 800 :height 700))
(defn -main []
  (do
    (reset! root-atoms [(-> f show!)])
    (native!)
    (config! f :content (new-db-connect-panel f))))









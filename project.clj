(defproject cljmsadmin "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :source-paths ["src/clj"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["lib/*"]
  ;:jvm-opts ["-Djava.library.path=C:\\sqlauth\\"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 ;Actually copied to /lib for version 4.0
                 ;[com.microsoft/sqljdbc4 "3.0"]
                 [seesaw "1.4.0"]
                 [hiccup "1.0.5"]
                 ]
  :main [cljmsadmin.core]
   :plugins [
             ]
   ;; ring tasks configuration
   ;:ring {:handler cljmsdoc.core/handler}

 ;  :git-dependencies [["https://github.com/clojure/core.typed.git"]]
 ;  :source-paths [".lein-git-deps/core.typed/src/main/clojure"]
  )


# cljMSDocument

## What it is
cljMsDocument is a tool to create data dictionaries for Microsoft T-SQL databases, written in Clojure.

## Great. So what's a data dictionary?
A database is made up of tables and fields. While the names of these provide some hints about what purpose the tables serve, something a bit more verbose than a single word is necessary to document something effectively. 

##How it works
Microsoft T-SQL databases offer extended properties that can be added to any part of the database structure. This project displays database schemas, tables and fields within a table and allows a user to add or edit the extended property named "MS_Description" on each of these objects. There is also an "Export" button which will dump the database structure including field names, types, and foreign key constraints and output it in an html format.

##Can this be used for a different database type?
I built the system so that all the database specific stuff is isolated to the src file "queries.clj" If you want to extend the project to a different database format, that should be the only file that needs to be touched.

## License
Copyright 2014 Peter Siewert

Distributed under the Eclipse Public License, the same as Clojure.


pushd p:\Projects\datomic-pro-0.9.5344
START bin\transactor p:\Projects\datomic-pro-0.9.5344\config\samples\dev-transactor-template.properties
START bin\rest -p 8001 -o http://localhost dev datomic:dev://localhost:4334/

POPD
lein exec create-data.clj
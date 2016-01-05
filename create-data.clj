(use '[leiningen.exec :only (deps)])
(deps '[[org.clojure/clojure "1.7.0"] [seesaw "1.4.4"]])

(deps '[[com.datomic/datomic-pro "0.9.5344"]]
      :repositories {"my.datomic.com" {:url   "https://my.datomic.com/repo"
                                   :username "truhtanov@targetprocess.com"
                                   :password "4f93c1ad-e2cf-4cb4-908f-795fe59a835d"}} )

(#'clojure.core/load-data-readers)
(set! *data-readers* (.getRawRoot #'*data-readers*))

(use 'clojure.pprint)
(require '[datomic.api :as d])



(def uri "datomic:dev://localhost:4334/targetprocess")


(d/delete-database uri)

(pprint "database targetprocess deleted")

(def res (d/create-database uri))
(pprint "database targetprocess created")
(pprint res)



(def conn (d/connect uri))
(pprint "connection created")
(def schema-tx (clojure.core/read-string  (slurp "schema.edn")))

@(d/transact conn schema-tx)
(pprint "schema created")

(def data-tx (clojure.core/read-string (slurp "data.edn")))
@(d/transact conn data-tx)
(pprint "data created")
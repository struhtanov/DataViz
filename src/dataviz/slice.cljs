(ns dataviz.slice
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [datomic-cljs.api :as d]
            [cljs.core.async :refer [<!]]
            [dataviz.utils :as utils])
  )

(enable-console-print!)

(defn build-tp-db-schema
  []
  {
   :db/id          {:db/axis :db.axis/none}
   :entity/name    {:db/axis :db.axis/none :db/card :db.card/available}
   :entity/project {:db/axis :db.axis/available :db/axis-name "project" :db/card :db.card/available}
   :entity/type    {:db/axis :db.axis/none :db/card :db.card/available}
   }
  )

(defn load-from-datomic
  []
  (go
    (let [conn (d/connect "localhost" 8001 "dev" "targetprocess")
          db (d/db conn)
          entityIds (<! (d/q '[:find ?e :where [?e :entity/type :entity.type/userStory]]
                             db))

          entities (map (fn [[entityId]]
                          (d/entity db entityId)
                          ) entityIds)

          ]
      (<! (cljs.core.async/into [] (cljs.core.async/merge entities)))
      )
    )
  )

(defn get-possible-axes []
  ;returns {:entity/project "project"}
  (map (fn [[k v]] [k (:db/axis-name v)])
       (filter
         (fn [[k v]]
           (= (:db/axis v) :db.axis/available))
         (build-tp-db-schema))))

(defn get-not-available-card-attributes []
  (keys
    (filter
      (fn [[k v]]
        (not= (:db/card v) :db.card/available))
      (build-tp-db-schema))))

(defn create-slice [data, x, y]
  (defn get-axis [axis-id]
    (def axis-values (distinct (map (fn [entity]
                                      (get entity axis-id)
                                      ) data)))
    (cons (utils/get-none) (remove #(= % nil) axis-values))
    )

  (defn get-cells [x y]
    (def cells-data (map (fn [c]
                           (def cardData (apply dissoc c (keyword x) (keyword y) (get-not-available-card-attributes)))
                           {
                            :x    (utils/none-if-nil (get c (keyword x)) identity)
                            :y    (utils/none-if-nil (get c (keyword y)) identity)
                            :data cardData
                            }
                           )
                         data
                         )
      )
    cells-data
    )
  (def xaxis (get-axis x))
  (def yaxis (get-axis y))
  (def cells (get-cells x y))


  (def res {:xaxis {:id x :values xaxis} :yaxis {:id y :values yaxis} :cells cells})
  res
  )
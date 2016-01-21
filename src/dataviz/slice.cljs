(ns dataviz.slice
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [datomic-cljs.api :as d]
            [cljs.core.async :refer [<!]]
            [cljs.core.async :refer [>!]]
            [dataviz.utils :as utils])
  )

(enable-console-print!)

(defn load-from-datomic
  []
  (go
    (let [conn (d/connect "localhost" 8001 "dev" "targetprocess")
          db (d/db conn)
          entityIds (<! (d/q '[:find ?e ?projection-axis ?projection-value
                               :where [?e :entity/axesProjections ?axes-projections]
                               [?axes-projections :axisProjection/axis ?projection-axis]
                               [?axes-projections :axisProjection/value ?projection-value]]
                             db))

          grouped-by-entity-id (for [[entity-id axis-projection-vec] (group-by first entityIds)]
                                 {:entity/id              entity-id
                                  :entity/axesProjections (apply merge (map (fn [[entity-id projection-axis projection-value]]
                                                                 {projection-axis projection-value}) axis-projection-vec))})

          ;entities is a collection of channels
          entities-channels (map (fn [entityId]
                                   ;entity is a channel
                                   (d/entity db entityId)
                                   ) (->> entityIds (map #(first %)) distinct))
          ]

      (def entities (<! (cljs.core.async/into [] (cljs.core.async/merge entities-channels))))

      (defn replace-projection-ids-with-values [entity] (update-in entity [:entity/axesProjections]
                                                                   (fn [projections]
                                                                     (def entity-id-with-projections (first (filter #(= (% :entity/id) (entity :db/id)) grouped-by-entity-id)))
                                                                     (entity-id-with-projections :entity/axesProjections)
                                                                     )))
      (def res (map replace-projection-ids-with-values entities))
      res
      )
    )
  )



(defn get-possible-axes []
  (go
    (let [conn (d/connect "localhost" 8001 "dev" "targetprocess")
          db (d/db conn)
          axes (<! (d/q '[:find ?axis ?axis-name
                          :where [?axis :axis/name ?axis-name]]
                        db))
          ]
      (into [[utils/no-axis-id "--- select axis ---"]] axes)
      )
    )
  )

(defn get-axis-values [axis-id]
  (print axis-id)
  (if (= axis-id utils/no-axis-id)
    (let [empty-channel (cljs.core.async/chan)]
      (cljs.core.async/put! empty-channel [[utils/no-axis-value-id "No value"]])
      empty-channel
      )
    (go
      (let [conn (d/connect "localhost" 8001 "dev" "targetprocess")
            db (d/db conn)
            axis-values (<! (d/q '[:find ?e ?entity-name
                                   :in $ ?axis-id
                                   :where [?e :entity/axis ?axis-id]
                                   [?e :entity/name ?entity-name]]
                                 db, axis-id))
            ]
        axis-values
        )
      )
    )
  )
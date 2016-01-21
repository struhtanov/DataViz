(ns dataviz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [dataviz.ui :as ui]
    [dataviz.slice :as slice]
    [cljs.core.async :refer [<!]]
    [dataviz.utils :as utils])
  )

(enable-console-print!)

(defn ^:export start
  []
  (go
    (def possible-axes (<! (slice/get-possible-axes)))
    (print possible-axes)

    (def entities (<! (slice/load-from-datomic)))
    (print "---------------------")

    (defn update-board [x-id y-id]
      (go
        (def xaxis-values (<! (slice/get-axis-values x-id)))
        (def yaxis-values (<! (slice/get-axis-values y-id)))
        (def cells-data (map (fn [entity]
                               (def projections (entity :entity/axesProjections))
                               (def xaxis-value (projections x-id))
                               (def yaxis-value (projections y-id))

                               {
                                ;x is a value of card on x-axis.
                                :x    (utils/no-value-if-nil xaxis-value identity)
                                :y    (utils/no-value-if-nil yaxis-value identity)
                                :data (dissoc entity :entity/axesProjections)
                                }
                               )
                             entities
                             ))

        (defn get-axis-name [axis-id] ((into {} possible-axes) axis-id))
        (def board {:xaxis {:id x-id :name (get-axis-name x-id) :values xaxis-values}
                    :yaxis {:id y-id :name (get-axis-name y-id) :values yaxis-values}
                    :cells cells-data})
        (print board)
        (ui/render-board possible-axes board update-board)
        )
      )

    (def xvalue (first possible-axes))
    (def yvalue (last possible-axes))
    (update-board (xvalue 0) (yvalue 0))
    )
  )
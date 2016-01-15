(ns dataviz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [dataviz.ui :as ui]
    [dataviz.slice :as slice]
    [cljs.core.async :refer [<!]])
  )

(enable-console-print!)

(defn ^:export start
  []
  (def possible-axes (slice/get-possible-axes))
  (go
    (def entities (<! (slice/load-from-datomic)))

    (defn update-board [x y]
      (def slice (slice/create-slice entities (keyword x) (keyword y)))
      (ui/render-board possible-axes slice update-board)
      )

    (update-board ((first possible-axes) 0) ((last possible-axes) 0))
    )
  )
(ns dataviz.utils)
(defn no-value-if-nil [v projector]
  (if (nil? v) no-axis-value-id (projector v))
  )
(def no-axis-id -1)
(def no-axis-value-id -1)


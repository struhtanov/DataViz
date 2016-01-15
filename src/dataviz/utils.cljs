(ns dataviz.utils)
(defn none-if-nil [v projector]
	(if (nil? v) "none" (projector v))
)
(defn get-none [] "none")
(defn none? [v] ( = v "none"))

(defn keyword-to-str [k] (subs (str k) 1))
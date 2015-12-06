(ns dataviz.ui
  (:require
    [quiescent.core :as q :include-macros true]
    [quiescent.dom :as d]
    [quiescent.dom.uncontrolled :as du]
    [cljsjs.fixed-data-table]
    [cljs.core.async :as async]))

;;; calling a React component directly is deprecated since v0.12
;;; http://fb.me/react-legacyfactory
(def Table (js/React.createFactory js/FixedDataTable.Table))
(def Column (js/React.createFactory js/FixedDataTable.Column))
(def ColumnGroup (js/React.createFactory js/FixedDataTable.ColumnGroup))

(enable-console-print!)

(q/defcomponent Header
  []
  (d/header {:id "header"}
            (d/h1 {} "DataViz")))

(q/defcomponent Footer
  []
  (d/footer {:id "footer"}
            (d/p {} "(c) TauCrew 2015")))

(q/defcomponent Option
  [item is-selected?]
  (d/option {:value item :selected is-selected?}
            item
  )
)

(q/defcomponent Selector
  [val]
  (def items (:items val))
  (def value (:value val))
  (def onChg (fn [e]
                 ((:onChange val) (.-value (.-currentTarget e)))
             ))
  (apply d/select
           {
             :className "selector"
             :onChange onChg
           }
           (map (fn [x] (Option x (= x value)))
                items)
  )
)

(q/defcomponent PanelWizard
  [full-state]
  (d/section {:id "panel-wizard"}
             (d/div {}
                    "Here you will be able to select "
                    (Selector {
                                :items (:xs full-state)
                                :value (:x? full-state)
                                :onChange #((:update full-state) %1 (:y? full-state))
                              })
                    " as X"
                    " and "
                    (Selector {
                                :items (:ys full-state)
                                :value (:y? full-state)
                                :onChange #((:update full-state) (:x? full-state) %1)
                              })
                    " as Y"
             )
  ))

(defn getter [k row]
  (nth row k))

(defn prepare-column [indx label]
  (Column #js {:label label :fixed true  :dataKey indx :cellDataGetter getter :width col-width}))

(q/defcomponent PanelResult
  [full-state]
  (def doc-width (.-clientWidth js/document.body))
  (def col-width (/ doc-width (+ (count (:xvalues full-state)) 1)))
  (def column-labels (conj (:xvalues full-state) 
    (str (:y? full-state) "/" (:x? full-state))))
  (def columns (map-indexed prepare-column column-labels))
  (defn get-row [k]
    (def h (nth (:yvalues full-state) k))
    (conj (range 0 (count (:xvalues full-state))) h))
  (d/section {:id "panel-result"}
             (Table
                #js {:width        doc-width
                     :height       600
                     :rowHeight    200
                     :rowGetter    get-row
                     :rowsCount    (count (:yvalues full-state))
                     :headerHeight 50}
                columns
             )
  ))

(q/defcomponent Content
  [full-state]
  (d/section {:id "content"}
             (PanelWizard full-state)
             (PanelResult full-state)
  ))

(q/defcomponent Board
  [full-state]
  (d/div {}
         (Header)
         (Content full-state)
         (Footer)
  ))

(defn board-state
  [schema data trigger-update]
    {
       :xs (conj schema "none")
       :ys (conj schema "none")
       :x? (:id (:xaxis data))
       :y? (:id (:yaxis data))
       :xvalues (conj (:values (:xaxis data)) "none")
       :yvalues (conj (:values (:yaxis data)) "none")
       :update trigger-update
    })

(def state-atom (atom {:github-input "pavel-drobushevich/DataViz"
                       :travisci-input "test"}))

(q/defcomponent Home
  [props]
  (d/div {}
         (Header)
         (d/span {:id "github"} (d/strong {} "GitHub repo: "))
         (du/input {:value (:github-input @state-atom)
              :style {:margin "10px"}
              :onChange (fn [evt]
                          (swap! state-atom assoc :github-input
                                 (.-value (.-target evt))))})
         (d/button {:onClick (fn [_]
                                ((:choose props) :github (:github-input @state-atom))
                              )}
            "Viz It!")
         (Footer)))

(defn render-board
  [schema data trigger-update]
  (q/render (Board (board-state schema data trigger-update)) js/document.body))

(defn render-home
  [choose]
  (q/render (Home {:choose choose}) js/document.body))

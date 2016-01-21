(ns dataviz.ui
  (:require
    [quiescent.core :as q :include-macros true]
    [quiescent.dom :as d]
    [quiescent.dom.uncontrolled :as du]
    [cljsjs.fixed-data-table]
    [dataviz.utils :as utils]))

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
                (d/option {:value (item 0) :selected is-selected?}
                          (item 1)
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
                        :onChange  onChg
                        }
                       (map (fn [x] (Option x (= (x 0) value)))
                            items)
                       )
                )

(q/defcomponent CellText
                [colVal, colIndex, fullRow]
                (d/div {:className "tick"}
                       colVal))

(q/defcomponent Card
                [cardData]
                (d/div {:className "card"}
                       (map #(d/div {} (str %1)) (vals cardData))
                       )
                )

(q/defcomponent CellCards
                [colVal, colIndex, fullRow]
                (apply d/div
                       {}
                       (map (fn [colItem] (Card (:data colItem))) colVal))
                )

(q/defcomponent PanelWizard
                [full-state]
                (d/section {:id "panel-wizard" :className "panel-wizard"}
                           (d/div {}
                                  "Select "
                                  (Selector {
                                             :items    (:xs full-state)
                                             :value    (:x-id full-state)
                                             :onChange #((:update full-state) (js/parseInt %1) (:y-id full-state))
                                             })
                                  " as X \u2192"
                                  " and "
                                  (Selector {
                                             :items    (:ys full-state)
                                             :value    (:y-id full-state)
                                             :onChange #((:update full-state) (:x-id full-state) (js/parseInt %1))
                                             })
                                  " as Y \u2193"
                                  )
                           ))

(defn getter [k row]
  (nth row k))

(defn prepare-column [indx col-name]
  (Column #js {
               :label          col-name
               :fixed          true
               :dataKey        indx
               :cellDataGetter getter
               :cellRenderer   (if (= indx 0) CellText CellCards)
               :width          col-width
               }))

(q/defcomponent PanelResult
                [full-state]
                (def doc-height (- (.-innerHeight js/window) 144))

                (def row-height (/ doc-height (count (:yvalues full-state))))
                (def doc-width (.-clientWidth js/document.body))
                (def col-width (/ (- doc-width 16) (+ (count (:xvalues full-state)) 1)))

                (defn get-axis-value-name [axis-value] (axis-value 1))
                (defn get-axis-value-id [axis-value] (axis-value 0))

                (def column-labels
                  (into [(str (:y-name full-state) "/" (:x-name full-state))]
                        (map get-axis-value-name (:xvalues full-state))
                        ))

                (def columns (map-indexed prepare-column column-labels))
                (defn get-row [k]
                  (def x-vals (:xvalues full-state))
                  (def y-vals (:yvalues full-state))
                  (def cells (:cells full-state))

                  (def row-id (get-axis-value-id (nth y-vals k)))
                  (def row-name (get-axis-value-name (nth y-vals k)))
                  (def row-cells (doall (filter #(= (:y %) row-id) cells)))

                  (def row (doall (map (fn [x-val]
                                         (filter #(= (:x %) (get-axis-value-id x-val)) row-cells))
                                       x-vals)))
                  (into [row-name] row)
                  )
                (d/section {:id "panel-result"}
                           (Table
                             #js {:width        doc-width
                                  :height       doc-height
                                  :rowHeight    row-height
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
                       ;;(Header)
                       (Content full-state)
                       ;;(Footer)
                       ))

(defn get-board-state
  [axes-meta data trigger-update]
  {
   :xs      axes-meta
   :ys      axes-meta
   :x-id    (:id (:xaxis data))
   :x-name  (:name (:xaxis data))
   :y-id    (:id (:yaxis data))
   :y-name  (:name (:yaxis data))
   :xvalues (:values (:xaxis data))
   :yvalues (:values (:yaxis data))
   :cells   (:cells data)
   :update  trigger-update
   })

(def state-atom (atom {:source-type "github"
                       :input       "TargetProcess/tauCharts"}))

(q/defcomponent Home
                [props]
                (d/div {:className "start-screen"}
                       ;;(Header)
                       (Selector {:items    `("github" "travis")
                                  :value    (:source-type @state-atom)
                                  :onChange #(swap! state-atom assoc :source-type %)})
                       (du/input {
                                  :placeholder "enter public repo"
                                  :value       (:input @state-atom)
                                  :style       {:margin "10px"}
                                  :onChange    (fn [evt]
                                                 (swap! state-atom assoc :input
                                                        (.-value (.-target evt))))})
                       (d/button {:onClick (fn [_]
                                             ((:choose props) (:source-type @state-atom) (:input @state-atom)))}
                                 "Explore!")
                       ;;(Footer)
                       ))

(defn render-board
  [axes-meta data trigger-update]
  (def board-state (get-board-state axes-meta data trigger-update))
  (q/render (Board board-state) js/document.body))

(defn render-home
  [choose]
  (q/render (Home {:choose choose}) js/document.body))

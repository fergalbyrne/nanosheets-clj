(ns igc.util
  (:import [java.awt Color Font])
  (:use [incanter core io stats charts datasets latex])
)

(def machine-dir (atom "../Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml"))
(def machine-file (atom (str @machine-dir "/SA-ref-100mg.csv")))

(defn set-machine-location!
  [d f]
  (reset! machine-dir d)
  (reset! machine-file (str @machine-dir "/" f)))

; (.setDomainGridlinePaint (.getXYPlot p) (Color. 240 240 240))

(defn get-xy-plot
  [chart]
  (.getXYPlot chart))

(defn set-domain-grid-color
  [xy-plot r g b]
  (let [color (Color. r g b)]
    (.setDomainGridlinePaint xy-plot color)))

(defn set-plot-theme
  [chart & {:keys [base-size] :or {base-size 12}}]
  (let [xy-plot (get-xy-plot chart)
        title (.getTitle chart)
        range-axis (.getRangeAxis xy-plot)
        domain-axis (.getDomainAxis xy-plot)
        legend (.getLegend chart)
        grid-grey (Color. 240 240 255)
        jet-black (Color. 0 0 0)
        arial12 (Font. "Arial" Font/PLAIN (* base-size 1.0))
        arial18 (Font. "Arial" Font/PLAIN (* base-size 1.75))
        arial36 (Font. "Arial" Font/PLAIN (* base-size 3.0))
        arial24 (Font. "Arial" Font/PLAIN (* base-size 2.0))
        ]
    (doto xy-plot
      (.setDomainGridlinePaint grid-grey)
      (.setRangeGridlinePaint grid-grey)
      (.setBackgroundPaint (Color. 255 255 255))
      (.setBackgroundAlpha 1.0)
      (.setOutlineVisible true)
      )
    (doto domain-axis
      (.setLabelPaint jet-black)
      (.setLabelFont arial24)
      (.setTickLabelPaint jet-black)
      (.setTickLabelFont arial18))
    (doto range-axis
      (.setLabelPaint jet-black)
      (.setLabelFont arial24)
      (.setTickLabelPaint jet-black)
      (.setTickLabelFont arial18))
    (if (nil? legend)
      nil
      (doto legend
      (.setItemPaint jet-black)
      (.setItemFont arial18)))
    (doto title
      (.setPaint jet-black)
      (.setFont arial36)))
  (doto chart
    ;(clear-background)
    (.setBackgroundPaint (Color. 250 250 250))))

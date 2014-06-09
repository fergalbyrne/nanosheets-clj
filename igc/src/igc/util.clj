(ns igc.util
  (:import [java.awt Color])
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
  [chart]
  (let [xy-plot (get-xy-plot chart)
        grid-grey (Color. 240 240 255)
        ]
    (doto xy-plot
      (.setDomainGridlinePaint grid-grey)
      (.setRangeGridlinePaint grid-grey)
      (.setBackgroundPaint (Color. 255 255 255))
      (.setBackgroundAlpha 1.0)
      ))
  (doto chart
    ;(clear-background)
    (.setBackgroundPaint (Color. 250 250 250))))

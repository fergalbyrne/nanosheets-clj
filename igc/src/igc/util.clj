(ns igc.util
  (:import [java.awt Color Font])
  (:require [clojure.string :as s])
  (:use [incanter core io stats charts datasets latex])
)

(defn title-case [string]
  (->> (s/split string #"\s+") (map s/capitalize) (s/join " ")))

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


(def color-list
  (cycle [[64 135 115]  ; olive green
          [184 75 95]   ; scarlet
          [84 75 195]   ; navy
          [98 54 12]    ; brown
          [34 185 185]  ; sea green
          [206 99 13]   ; orange
          ]))

(defn set-series-color
  [chart i-renderer i-series r g b]
  (let [color (Color. r g b)]
    (.setSeriesPaint (.getRenderer (.getXYPlot chart) i-renderer) i-series color)))

(defn fix-colors
  [chart n]
  (let [colors (vec (take n color-list))]
    (reduce (fn [chart i]
              (let [[r g b] (nth colors i)]
                (set-series-color chart i 0 r g b)
                chart))
            chart (range n))))

(defn set-plot-theme
  [chart & {:keys [base-size set-min set-max] :or {base-size 12 set-min false set-max false}}]
  (let [xy-plot (get-xy-plot chart)
        n-series (.getDatasetCount xy-plot)
        title (.getTitle chart)
        range-axis (.getRangeAxis xy-plot)
        _ (when-not (false? set-min) (.setLowerBound range-axis set-min))
        _ (when-not (false? set-max) (.setUpperBound range-axis set-max))
        domain-axis (.getDomainAxis xy-plot)
        legend (.getLegend chart)
        grid-grey (Color. 240 240 255)
        jet-black (Color. 0 0 0)
        arial12 (Font. "Arial" Font/PLAIN (* base-size 1.0))
        arial18 (Font. "Arial" Font/PLAIN (* base-size 1.65))
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
      (.setFont arial36))
    (doto chart
      ;(clear-background)
      (fix-colors n-series)
      (.setBackgroundPaint (Color. 250 250 250)))))

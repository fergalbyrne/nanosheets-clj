(ns igc.surface-energy
  (:use [incanter core io stats charts datasets])
  (:require [me.raynes.fs :as fs]
            [clojure.string :as s])
  (:gen-class :main true))

(def machine-dir (atom "../Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml"))
(def machine-file (atom (str @machine-dir "/SA-ref-100mg.csv")))

(defn save-plot
  [plot m-dir plot-tag]
  (let [fname (str m-dir "/" plot-tag "--" (fs/base-name m-dir) ".png")]
    (println "Saving plot to" fname)
    (save plot fname :width 1600 :height 1200)))

(comment
  (set-machine-location! "../Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S1-SA-10ml" "SA-2k-12mg-S1.csv")
  (write-se-data-files @machine-file @machine-dir)
  (def p (bet-plot @machine-dir :amount-mmol))
  (view p)
  (save-plot p @machine-dir "BET-mmol")
  (def p (injection-items-plot @machine-dir :partial-pressure :net-ret-vol-max "Volume [max] vs Partial Pressure"))
  (view p)
  (save-plot p @machine-dir "Volume-max-vs-Partial-Pressure")
  (def p (injection-items-plot @machine-dir :partial-pressure :net-ret-vol-com "Volume [com] vs Partial Pressure"))
  (view p)
  (save-plot p @machine-dir "Volume-com-vs-Partial-Pressure")
  (def p (injection-items-plot @machine-dir :actual-surface-coverage :net-ret-vol-max "Volume [max] vs Actual Surface Coverage"))
  (view p)
  (save-plot p @machine-dir "Volume-max-vs-Actual-Surface-Coverage")
  (def p (injection-items-plot @machine-dir :actual-surface-coverage :net-ret-vol-com "Volume [com] vs Actual Surface Coverage"))
  (view p)
  (save-plot p @machine-dir "Volume-com-vs-Actual-Surface-Coverage")

  (set-machine-location! "../Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S2-SA-10ml" "SA-2k-12mg-S2.csv")
  (set-machine-location! "../Nanosheets-Prep-II-4000rpm-3mg-01-3mm-30C-S1-SA-10ml" "SA-4k-3mg.csv")

  )

(defn set-machine-location!
  [d f]
  (reset! machine-dir d)
  (reset! machine-file (str @machine-dir "/" f)))

(defn tag-data [data col tag]
	(add-derived-column col [] #(str tag) data))

(def igc-dir (fs/file "../01-Access-Files"))

(defn glob-pattern [dir pattern]
  (fs/with-mutable-cwd (fs/chdir dir) (fs/glob pattern)))

(def substances (glob-pattern igc-dir "*"))

(defn treatments [substance] (glob-pattern substance "*"))

(defn treatments [substance] (glob-pattern substance "*"))

(defn clean-file-name
  [file pattern repl]
  (-> file
      fs/base-name
      (s/replace pattern repl)
      (s/replace "-" " ")))

(def se-headers ["SEA Analysis Data Result"
                 "Free Energy"
                 "Dispersive Surface Energy"
                 "Injection Items"])

(defn title->keyword [t]
  (keyword (s/replace (s/lower-case (str t)) #"\s+" "-")))

(comment
(def machine-file
  "../Nanosheets-Prep-I-3000rpm-8mg-03-2mm-50C-S1-SE-10ml-redone/SE-3krpm-8mg.csv")
(def machine-dir
  "../Nanosheets-Prep-I-3000rpm-8mg-03-2mm-50C-S1-SE-10ml-redone")


(def machine-file
  "../Nanosheets-Prep-I-3000rpm-8mg-01-3mm-30C-S1-SA-10ml/SA-3k-8mg.csv")
;SA-3k-8mg
(def machine-dir
  "../Nanosheets-Prep-I-3000rpm-8mg-01-3mm-30C-S1-SA-10ml")

(def machine-file
  "../Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml/SA-ref-100mg.csv")

(def machine-dir
  "../Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml")
  )


(defn split-surface-energy-file
  [f]
  (with-open [r (clojure.java.io/reader f :encoding "UTF-16")]
    (loop [current (first se-headers)
           remain (rest se-headers)
           writing current
           line-map {}]
      (let [line (.readLine r)
            header? (and (not (nil? line))
                         (not (nil? current))
                         (string? line)
                         (.startsWith line current))
            writing (if header? current writing)
            current (if header? (first remain) current)
            remain (if header? (rest remain) remain)
            kcurrent (title->keyword writing)
            line-map (if header?
                       (conj line-map {kcurrent []})
                       (update-in line-map [kcurrent] conj line))]
        #_(if (not header?)
          (println writing (first remain) header? line)
          (println ">>> Match: expecting" current "next"))
        (if (nil? line)
          line-map
          (recur current remain writing line-map))))))

(defn write-se-data-files [f out-dir]
  (let [data-map (split-surface-energy-file f)]
    (for [id (keys data-map)]
      (let [outfile (str out-dir "/se-data-" (name id) ".csv")]
        (println "Writing" id "to" outfile)
        (with-open [w (clojure.java.io/writer outfile)]
          (doseq [line (data-map id)]
            (if (nil? line)
              (println "..")
              (do (.write w line)
                  (.newLine w)))))))))

;(write-se-data-files machine-file machine-dir)

(def free-energy-cols {0 :n-nm
                           1 :solvent-name
                           2 :units
                           3 :en-stz-max
                           4 :en-stz-com
                           5 :en-pol-max
                           6 :en-pol-com
                           7 :int-retention-vol-max
                           8 :int-retention-vol-com
                           9 :col-temp})
(def free-energy-titles {:n-nm "n/n_m"
                         :int-retention-vol-max "Interpolated Retention Volume (max)"
                         :int-retention-vol-com "Interpolated Retention Volume (com)"})

;Dispersive Surface Energy
;n/nm	Disp. Surf. En. (mJ/m^2) - Stz & Max	Disp. Surf. En. (mJ/m^2) - Stz & Com	Disp. Surf. En. (mJ/m^2) - DnG & Max	Disp. Surf. En. (mJ/m^2) - DnG & Com

;Injection Items
;ID	Injection Name	Solvent	Injection Time [ms]	Duration [min]
;Target Fractional Surface Coverage	Actual Fractional Surface Coverage
;Column Temperature [Kelvin]	Column Pressure Drop [Torr]
;Exit Flow Rate [sccm]
;Peak Area [µV•min]	Peak Max (Signal) [µV]	Peak Max (Time) [min]	Peak Com [min]	Peak Com/Max	Ret Volume (Max) [ml/g]	Ret Volume (Com) [ml/g]	Net Ret Time (Max) [min]	Net Ret Volume (Max) [ml/g]	Net Ret Time (Com) [min]	Net Ret Volume (Com) [ml/g]	Partial Pressure [Torr]	Pres Ret Volume (Max) [mMol/(g•Torr)]	Pres Ret Volume (Com) [mMol/(g•Torr)]	Amount  [mMol/g]	Amount  [mMol]	Amount (Max) [mMol/g]	Amount (Com) [mMol/g]

(def injection-items-cols {0 :id
                           1 :injection-name
                           2 :solvent-name
                           3 :injection-time
                           4 :duration
                           5 :target-surface-coverage
                           6 :actual-surface-coverage
                           7 :col-temp
                           8 :pressure-drop
                           9 :exit-flow-rate
                           10 :peak-area
                           11 :peak-max-signal
                           12 :peak-max-time
                           13 :peak-com
                           14 :peak-com-over-max
                           15 :ret-vol-max
                           16 :ret-vol-com
                           17 :net-ret-time-max
                           18 :net-ret-vol-max
                           19 :net-ret-time-com
                           20 :net-ret-vol-com
                           21 :partial-pressure
                           22 :pres-res-vol-max
                           23 :pres-res-vol-com
                           24 :amount-mmol-g
                           25 :amount-mmol
                           26 :amount-mmol-g-max
                           27 :amount-mmol-g-com})

(def injection-items-titles {:id "ID"
                             :injection-name "Injection Name"
                             :solvent-name "Solvent"
                             :injection-time "Injection Time [ms]"
                             :duration "Duration [min]"
                             :target-surface-coverage "Target Fractional Surface Coverage"
                             :actual-surface-coverage "Actual Fractional Surface Coverage"
                             :col-temp "Column Temperature [Kelvin]"
                             :pressure-drop "Column Pressure Drop [Torr]"
                             :exit-flow-rate "Exit Flow Rate"
                             :peak-area "Peak Area"
                             :peak-max-signal "Peak Max Signal"
                             :peak-max-time "Peak Max Time"

                             :net-ret-vol-max "Net Retention Volume (max) [ml/g]"
                             :net-ret-vol-com "Net Retention Volume (com) [ml/g]"
                             :partial-pressure "Partial Pressure"
                             :amount-mmol-g-max "Amount (max) [mMol/g]"
                             :amount-mmol-g-com "Amount (com) [mMol/g]"
                             :bet-ordinate "p/p0"
                             :bet-value "p/ν(p0-p)"
                             })

(defn solvent-properties
  [data-dir key-col val-col]
  (let [data (read-dataset (str data-dir "/export/solventReservoirs.csv")
                     :header true)
        data-map (into {} ($map (fn [k v] {k v}) [key-col val-col] data))]
    data-map))


(defn bet-value [p p0 n] (/ p (* n (- p0 p))))

(defn bet-ordinate [p p0] (/ p p0))

(defn data-loader
  [data-dir fname col-map]
  (->> (read-dataset (str data-dir "/se-data-" fname ".csv")
                     :header true :delim \tab)
       (rename-cols col-map)))

(defn p0
  "Octane (in Torr)"
  [t]
  (* 101.325e3 ; standard pressure
     7.5e-3    ; convert to torr
     (Math/pow Math/E
               (/ (* 35462
                     (- (/ 1 398.8)
                        (/ 1 t)))
                  8.314))))

(defn plot-maker
  [data title-map x-col y-col grp-col title]
  (xy-plot x-col y-col :data data
             :group-by grp-col :legend false
             :x-label (title-map x-col)
             :y-label (title-map y-col)
             :title title
           :points true))

(defn free-energy-plot [data-dir xcol ycol title]
  (let [data (data-loader data-dir "free-energy" free-energy-cols)]
    (plot-maker data free-energy-titles
                xcol ycol :solvent-name title)))


(defn injection-items-plot [data-dir xcol ycol title]
  (let [data (data-loader data-dir "injection-items" injection-items-cols)
        data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                     data)]
  (plot-maker data injection-items-titles
              xcol ycol :solvent-name title)))

(defn bet-plot [data-dir amt-col]
  (let [solvent-data (solvent-properties data-dir :solvent_name :criticalPressure)
        data (data-loader data-dir "injection-items" injection-items-cols)
        data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                     data)
        data (->> data
                  (add-derived-column :criticalPressure
                                      ;[:solvent-name]
                                      []
                                      ;#(get solvent-data %)
                                      #(p0 303.15))
                  (add-derived-column :bet-value
                                      [:partial-pressure :criticalPressure amt-col]
                                      bet-value)
                  (add-derived-column :bet-ordinate
                                      [:partial-pressure :criticalPressure]
                                      bet-ordinate)
                  ($where ($fn [bet-ordinate](and (> bet-ordinate 0)
                                                  (< bet-ordinate 0.6)))))
        ;_ (view data)
        ]
    (plot-maker data injection-items-titles
              :bet-ordinate :bet-value :solvent-name "BET Plot")))

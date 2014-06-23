(ns igc.surface-energy
  (:use [incanter core io stats charts datasets latex])
  (:require [me.raynes.fs :as fs]
            [clojure.string :as s]
            [clojure.set :as sets]
            [igc.util :refer :all])
  (:gen-class :main true))



(def machine-file-list
  [
   [:code :ref-1-supplied-sa-s1-100mg
    :dir "Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml"
    :machine-file "SA-ref-100mg.csv"]
   [:code :ref-1-supplied-se-s1-100mg
    :dir "Reference-As-Supplied-100mg-3mm-50C-S1-SE-10ml"
    :machine-file "SE-ref-100mg.csv"]
   [:code :ref-1-supplied-sa-s3-13mg
    :dir "Reference-As-Supplied-13mg-2mm-100C-S3-SA-10ml"
    :machine-file "Reference-13mg-S3-SA-100C.csv"]
   [:code :ref-1-supplied-sa-s1-13mg
    :dir "Reference-As-Supplied-13mg-2mm-30C-S1-SA-10ml"
    :machine-file "Reference-13mg-S1-SA-30C.csv"]
   [:code :ref-1-supplied-sa-s3-13mg30C
    :dir "Reference-As-Supplied-13mg-2mm-30C-S3-SA-10ml"
    :machine-file "Reference-13mg-S3-SA-30C.csv"]
   [:code :ref-1-supplied-sa-s1-10mg
    :dir "Reference-As-Supplied-5mg-3mm-30C-S1-SA-10ml"
    :machine-file "ref-5mg-SA-30C-S1.csv"]
   [:code :nano-1-3000-se-s1-8mg
    :dir "Nanosheets-Prep-I-3000rpm-8mg-03-2mm-50C-S1-SE-10ml-redone"
    :machine-file "SE-3krpm-8mg.csv"]
   [:code :nano-1-1500-se-s1-10mg
    :dir "Nanosheets-Prep-I-1500rpm-10mg-01-3mm-50C-S1-SE-10ml"
    :machine-file "nanosheet-prep-I-1_5k-3mm-S1-SE-50C.csv"]
   [:code :nano-1-750-se-s2-20mg
    :dir "Nanosheets-Prep-I-750rpm-20mg-01-3mm-50C-S2-SE-10ml"
    :machine-file "nanosheet-750rpm-3mm-S2-SE-50C.csv"]
   [:code :nano-1-3000-sa-s1-8mg
    :dir "Nanosheets-Prep-I-3000rpm-8mg-01-3mm-30C-S1-SA-10ml"
    :machine-file "SA-3k-8mg.csv"]
   [:code :nano-1-1500-sa-s1-10mg
    :dir "Nanosheets-Prep-I-1500rpm-10mg-01-3mm-30C-S1-SA-10ml"
    :machine-file "nanosheet-Prep-I-1_5k-10mg-3mm-S1-SA-30C.csv"]
   [:code :nano-1-1500-sa-s1run2-10mg
    :dir "Nanosheets-Prep-I-1500rpm-10mg-01-3mm-30C-S1-SA-10ml-run2"
    :machine-file "nanosheet-Prep-I-1_5k-10mg-3mm-S1-SA-30C-run2.csv"]
   [:code :nano-1-1500-sa-s2-10mg
    :dir "Nanosheets-Prep-I-1500rpm-10mg-01-3mm-30C-S2-SA-10ml"
    :machine-file "nanosheet-Prep-I-1_5k-10mg-3mm-S2-SA-30C.csv"]
   [:code :nano-1-750-sa-s1-20mg
    :dir "Nanosheets-Prep-I-750rpm-20mg-01-3mm-30C-S1-SA-10ml"
    :machine-file "nanosheet-prep-I-750rpm-20mg-3mm-S1-SA-30C.csv"]
   [:code :nano-1-750-sa-s2-20mg
    :dir "Nanosheets-Prep-I-750rpm-20mg-01-3mm-30C-S2-SA-10ml"
    :machine-file "nanosheet-prep-I-750rpm-20mg-3mm-S2-SA-30C.csv"]
   [:code :nano-2-2000-sa-s1-12mg
    :dir "Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S1-SA-10ml"
    :machine-file "SA-2k-12mg-S1.csv"]
   [:code :nano-2-2000-sa-s2-12mg
    :dir "Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S2-SA-10ml"
    :machine-file "SA-2k-12mg-S2.csv"]
   [:code :nano-2-2000-se-s1-12mg
    :dir "Nanosheets-Prep-II-2000rpm-12mg-01-3mm-50C-S1-SE-10ml"
    :machine-file "SE-2krpm-12mg-S1.csv"]
   [:code :nano-2-4000-sa-s1-3mg
    :dir "Nanosheets-Prep-II-4000rpm-3mg-01-3mm-30C-S1-SA-10ml"
    :machine-file "SA-4k-3mg.csv"]
   [:code :nano-2-4000-se-s1-3mg
    :dir "Nanosheets-Prep-II-4000rpm-3mg-01-3mm-50C-S1-SE-10ml"
    :machine-file "nanosheet-4k-2mm-SE-50C.csv"]
   ])


(def machine-files (into {} (for [[_ code _ dir _ machine-file] machine-file-list]
                              {dir machine-file})))

(def machine-data (into {} (for [[_ code _ dir _ machine-file] machine-file-list]
                              {code {:dir dir :machine-file machine-file}})))


(defn save-plot
  [plot m-dir plot-tag & {:keys [width height] :or {width 720 height 720}}]
  (let [fname (str m-dir "/" plot-tag "--" (fs/base-name m-dir) ".png")]
    (println "Saving plot to" fname)
    (save (set-plot-theme plot) fname :width width :height height)))

(defn save-plot-data
  [data m-dir plot-tag]
  (let [fname (str m-dir "/" plot-tag "--" (fs/base-name m-dir) ".csv")]
    (println "Saving data to" fname)
    (save data fname)))

(def log->ln (Math/log10 Math/E))

(def antoine-params
  {"OCTANE" [13.9346 3123.13 209.635]
   "DICHLOROMETHANE" [13.9891 2463.93 223.240]
   "HEPTANE" [13.8622 2910.26 216.432]
   "HEXANE" [13.8193 2696.04 224.317]
   "NONANE" [13.9854 3311.19 202.694]
   "ETHYL ACETATE" [(/ 4.22809 log->ln) (/ 1245.702 log->ln) (+ 273.15 -55.189)]})

; log 10 for "ETHYL ACETATE" [4.22809	1245.702	-55.189]
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

(defn p0
  "Saturation Pressure (in Torr) of solvent at t degrees C"
  [t solvent]
  (let [;_ (println "p0" t solvent)
        [A B C] (antoine-params solvent)]
    ;(println "p0" t solvent A B C)
    (* (/ 760.0 101.325) ; convert to Torr
       (Math/pow Math/E (- A (/ B (+ t C)))))))


(defn tag-data [data col tag]
	(add-derived-column col [] #(str tag) data))

(def igc-dir (fs/file "../01-Access-Files"))
(def sem-dir (fs/file "../03-Processed-Files"))

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
                        :en-stz-max "Free Energy Schultz [max] (mJ/m^2)"
                        :en-stz-com "Free Energy Schultz [com] (mJ/m^2)"
                        :en-pol-max "Pol Energy [max] (mJ/m^2)"
                        :en-pol-com "Pol Energy [com] (mJ/m^2)"
                         :int-retention-vol-max "Interpolated Retention Volume (max)"
                         :int-retention-vol-com "Interpolated Retention Volume (com)"})

;Dispersive Surface Energy
;n/nm	Disp. Surf. En. (mJ/m^2) - Stz & Max	Disp. Surf. En. (mJ/m^2) - Stz & Com
;Disp. Surf. En. (mJ/m^2) - DnG & Max	Disp. Surf. En. (mJ/m^2) - DnG & Com

(def dispersive-cols {0 :n-nm
                      1 :en-stz-max
                      2 :en-stz-com
                      3 :en-dng-max
                      4 :en-dng-com})
(def dispersive-titles {:n-nm "n/n_m"
                        :en-stz-max "Disp. Surf. En. (mJ/m^2) - Stz & Max"
                        :en-stz-com "Disp. Surf. En. (mJ/m^2) - Stz & Com"
                        :en-dng-max "Disp. Surf. En. (mJ/m^2) - DnG & Max"
                        :en-dng-com "Disp. Surf. En. (mJ/m^2) - DnG & Com"
                        })

(def dispersive-legend-titles {:n-nm "n/n_m"
                        :en-stz-max "Schultz [max]"
                        :en-stz-com "Schultz [com]"
                        :en-dng-max "Dorris-Gray [max]"
                        :en-dng-com "Dorris-Gray [com]"
                        })
; n/nm	?d	?s+	?s-	?ab	?t

(def sem-cols {0 :n-nm
               1 :gamma-d
               2 :gamma-s-plus
               3 :gamma-s-minus
               4 :gamma-ab
               5 :gamma-t})

(def small-gamma "ɣ")
(def capital-gamma "Ɣ")

(def sem-titles {:n-nm "n/n_m"
                 :gamma-d "ɣD (mJ/m^2)"
                 :gamma-s-plus "ɣS+ (mJ/m^2)"
                 :gamma-s-minus "ɣS- (mJ/m^2)"
                 :gamma-ab "ɣAB (mJ/m^2)"
                 :gamma-t "ɣt (mJ/m^2)"
                 :total-gamma-plus "ɣD + ɣS+ (mJ/m^2)"
                 :total-gamma-minus "ɣD + ɣS+ + ɣS- (mJ/m^2)"
                 })

(def sem-legend-titles {:n-nm "n/n_m"
                 :gamma-d "ɣDispersive"
                 :gamma-s-plus "ɣS+ (acid)"
                 :gamma-s-minus "ɣS- (base)"
                 :gamma-ab "ɣAB"
                 :gamma-t "ɣtotal"
                 :total-gamma-plus "ɣD + ɣS+ (mJ/m^2)"
                 :total-gamma-minus "ɣD + ɣS+ + ɣS- (mJ/m^2)"
                 })

;Injection Items
;ID	Injection Name	Solvent	Injection Time [ms]	Duration [min]
;Target Fractional Surface Coverage	Actual Fractional Surface Coverage
;Column Temperature [Kelvin]	Column Pressure Drop [Torr]
;Exit Flow Rate [sccm]
;Peak Area [µV•min]	Peak Max (Signal) [µV]	Peak Max (Time) [min]
;Peak Com [min]	Peak Com/Max	Ret Volume (Max) [ml/g]	Ret Volume (Com) [ml/g]
;Net Ret Time (Max) [min]	Net Ret Volume (Max) [ml/g]	Net Ret Time (Com) [min]
;Net Ret Volume (Com) [ml/g]	Partial Pressure [Torr]	Pres Ret Volume (Max) [mMol/(g•Torr)]	Pres Ret Volume (Com) [mMol/(g•Torr)]	Amount  [mMol/g]	Amount  [mMol]	Amount (Max) [mMol/g]	Amount (Com) [mMol/g]

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
                             :actual-surface-coverage "Actual Coverage (n/nm)"
                             :col-temp "Column Temperature [Kelvin]"
                             :pressure-drop "Column Pressure Drop [Torr]"
                             :exit-flow-rate "Exit Flow Rate"
                             :peak-area "Peak Area"
                             :peak-max-signal "Peak Max Signal"
                             :peak-max-time "Peak Max Time"

                             :net-ret-vol-max "Net Retention Volume (max) [ml/g]"
                             :net-ret-vol-com "Net Retention Volume (com) [ml/g]"
                             :net-ret-time-max "Net Retention Time (max) [min]"
                             :net-ret-time-com "Net Retention Time (com) [min]"
                             :partial-pressure "Partial Pressure [Torr]"
                             :amount-mmol "Amount [mMol]"
                             :amount-mmol-g "Amount [mMol/g]"
                             :amount-mmol-g-max "Amount (max) [mMol/g]"
                             :amount-mmol-g-com "Amount (com) [mMol/g]"
                             :bet-ordinate "p/p0"
                             :bet-value "p/ν(p0-p)"
                             })

  (def codes [:nano-1-750-sa-s1-20mg
              :nano-1-750-sa-s2-20mg
              :nano-1-1500-sa-s2-10mg
              :nano-2-2000-sa-s1-12mg
              :nano-2-2000-sa-s2-12mg
              :nano-1-3000-sa-s1-8mg
              :nano-2-4000-sa-s1-3mg]
    )
    (def sa-codes [:nano-1-750-sa-s1-20mg
              :nano-1-750-sa-s2-20mg
              :nano-1-1500-sa-s2-10mg
              :nano-2-2000-sa-s1-12mg
              :nano-2-2000-sa-s2-12mg
              :nano-1-3000-sa-s1-8mg
              :nano-2-4000-sa-s1-3mg]
    )
    (def sa-labels
      {:nano-1-750-sa-s1-20mg "750rpm 20mg (S1)"
              :nano-1-750-sa-s2-20mg "750rpm 20mg (S2)"
              :nano-1-1500-sa-s2-10mg "1500rpm 10mg (S2)"
              :nano-2-2000-sa-s1-12mg "2Krpm 12mg (S1)"
              :nano-2-2000-sa-s2-12mg "2Krpm 12mg (S2)"
              :nano-1-3000-sa-s1-8mg "3Krpm 8mg (S1)"
              :nano-2-4000-sa-s1-3mg "4Krpm 3mg (S1)"
       }
    )
    (def se-codes [:ref-1-supplied-se-s1-100mg
               :nano-1-3000-se-s1-8mg
               :nano-1-1500-se-s1-10mg
               :nano-1-750-se-s2-20mg
               :nano-2-2000-se-s1-12mg
               :nano-2-4000-se-s1-3mg])

    (def se-labels {:ref-1-supplied-se-s1-100mg "Ref 100mg"
               :nano-1-3000-se-s1-8mg "Nano I 3Krpm"
               :nano-1-1500-se-s1-10mg "Nano I 1500rpm"
               :nano-1-750-se-s2-20mg "Nano I 750rpm"
                    :nano-2-2000-se-s1-12mg "Nano II 2Krpm"
               :nano-2-4000-se-s1-3mg "Nano II 4Krpm"})

(defn solvent-properties
  [data-dir key-col val-col]
  (let [data (read-dataset (str data-dir "/export/solventReservoirs.csv")
                     :header true)
        data-map (into {} ($map (fn [k v] {k v}) [key-col val-col] data))]
    data-map))

(def solvent-symbols
  (map keyword '[reservoirId solvent_name is_dispersive cross_sectional_area
                 dispersive_surface_tension molecular_mass liquid_density
                 boiling_pt criticalPressure]))

(defn solvent-table
  [data-dir]
  (let [data (read-dataset (str data-dir "/export/solventReservoirs.csv")
                     :header true)
        data (sel data :cols solvent-symbols)
        rows (s/join "\\\\\n  "
                     ($map (fn [& items]
                             (s/join " & " items)) solvent-symbols
                           data))]
    (str "
\\begin{table}[htb]
 \\begin{tabular}{l l c c c c c c l}
 \\toprule
  Id & Solvent  & Dispersive  & $a_\\mathrm{p}$ & ${\\Gamma_L}^D$ & Mol. Mass "
" & $\\rho_L$ & $T_B (^\\circ C)$ & $p_\\mathrm{crit}$ \\\\
  \\midrule
  "
rows
"\\\\
  \\bottomrule
 \\end{tabular}
\\caption{Solvent reservoir data}
\\end{table}")))


(defn bet-value [p p0 n] (/ p (* n (- p0 p))))

(defn bet-ordinate [p p0] (/ p p0))

(defn data-loader
  [data-dir fname col-map]
  (->> (read-dataset (str data-dir "/se-data-" fname ".csv")
                     :header true :delim \tab)
       (rename-cols col-map)))

(defn sem-loader
  [fname col-map]
  (->> (read-dataset fname :skip 10 :delim \tab)
       (rename-cols col-map)))

(defn plot-maker
  [data title-map x-col y-col grp-col title
   & {:keys [legend series-label] :or {legend false series-label (title-map y-col)}}]
  (xy-plot x-col y-col :data data
             :group-by grp-col
           :legend legend
             :x-label (title-map x-col)
             :y-label (title-map y-col)
             :title title
           :series-label series-label
           :points true))

(defn free-energy-plot [data-dir xcol ycol title]
  (let [data (data-loader data-dir "free-energy" free-energy-cols)]
    (plot-maker data free-energy-titles
                xcol ycol :solvent-name title)))


(defn injection-items-plot
  [data-dir xcol ycol title & {:keys [legend] :or {legend false}}]
  (let [data (data-loader data-dir "injection-items" injection-items-cols)
        data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                     data)
        data ($order :solvent-name :asc data)
        ;_ (view data)
        ]
  (plot-maker data injection-items-titles
              xcol ycol :solvent-name title :legend legend)))

(defn by-solvent
  [solvent data]
  ($where ($fn [solvent-name] (= solvent solvent-name)) data))

(defn solvents-plot
  [data-dir xcol ycol title & {:keys [legend] :or {legend false}}]
  (let [data (data-loader data-dir "injection-items" injection-items-cols)
        data (transform-col data :solvent-name title-case)
        data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                     data)
        data ($order :solvent-name :asc data)
        solvents (vec (set (sel data :cols :solvent-name)))
        ;_ (view data)
        ;_ (println solvents)
        solvent (first solvents)
        solvent-data (by-solvent solvent data)
        plot (plot-maker solvent-data injection-items-titles
              xcol ycol :solvent-name title :legend legend
                         :series-label (title-case solvent))
        ]
    (reduce (fn [p solvent]
                       (add-lines p xcol ycol
                                  :data (by-solvent solvent data)
                                  :series-label (title-case solvent)
                                  :points true))
                     plot
                     (rest solvents))))

(defn injection-plots
  [root codes labels xcol ycol title & {:keys [solvent] :or {solvent "OCTANE"}}]
  (let [dirs (map :dir (vals (select-keys machine-data codes)))
        ;_ (println dirs)
        first-dir (:dir ((first codes) machine-data))
        _ (set-machine-location! (str root "/" first-dir) (machine-files first-dir))
        data-sets (for [code codes]
                    (let [dir (:dir (code machine-data))
                          data-dir (str root "/" dir)
                          fname (fs/base-name data-dir)
                          _ (println "loading " fname)
                          data (data-loader data-dir "injection-items" injection-items-cols)
                          data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                                   data)
                          data ($where ($fn [solvent-name](= solvent-name solvent))
                                   data)
                          data (add-derived-column :dir [] (fn [] (labels code)) data)]
                      [code data]))
        first-data (first data-sets)
        plot (plot-maker (second first-data) injection-items-titles
              xcol ycol :dir title :legend true :series-label (labels (first codes)))]
    (reduce (fn [p data-tuple]
                       (add-lines p xcol ycol
                                  :data (second data-tuple)
                                  :series-label (labels (first data-tuple))
                                  :points true))
                     plot
                     (rest data-sets))))

(defn dispersive-plot [data-dir xcol ycol title]
  (let [data (data-loader data-dir "dispersive-surface-energy" dispersive-cols)
        ;_ (view data)
        ]
  (xy-plot xcol ycol :data data
             :legend false
             :x-label (dispersive-titles xcol)
             :y-label (dispersive-titles ycol)
             :title title
           :points true)))

(defn dispersive-plots [data-dir]
  (let [data (data-loader data-dir "dispersive-surface-energy" dispersive-cols)
        ;_ (view data)
        plot (xy-plot :n-nm :en-dng-com :data data
             :legend true
             :x-label (dispersive-titles :n-nm)
             :y-label "Dispersive Surface Energy (mJ/m^2)"
                      :series-label (dispersive-legend-titles :en-dng-com)
             :title "Dispersive Surface Energy"
           :points true)
        plot (reduce (fn [p col]
                       (add-lines p :n-nm col
                                  :data data
                                  :points true
                                  :series-label (dispersive-legend-titles col)))
                     plot
                     [:en-dng-max :en-stz-com :en-stz-max])
        plot (set-plot-theme plot :set-min 35.0)]
  plot))

(defn free-energy-plots [data-dir]
  (let [data (data-loader data-dir "free-energy" free-energy-cols)
        data ($where ($fn [solvent-name] (or (= solvent-name "DICHLOROMETHANE")
                                             (= solvent-name "ETHYL ACETATE")))
                     data)
        ;_ (view data)
        plot (xy-plot :n-nm :en-stz-com :data data
             :legend true
             :x-label (free-energy-titles :n-nm)
                      :group-by :solvent-name
             :y-label "Surface Energy dG (kJ/Mol)"
                      :series-label (free-energy-titles :en-stz-com)
             :title "Surface Energy"
           :points true)
        plot (set-plot-theme plot :set-min false)]
  plot))

(def avogadro 6.0221413e+23)
(defn RTlnV
  [tK V]
  (let [gas-constant 8.3145]
    (* gas-constant
       tK
       (Math/log V))))

(defn ap-sqrt-gamma
  [ap gamma]
  (* 2.0 ap avogadro (Math/sqrt gamma)))

(defn make-linear-model
  [lm-data xcol ycol]
  (let [lm-x (to-matrix (sel lm-data :cols [xcol]))
        lm-y (to-matrix (sel lm-data :cols [ycol]))
        lm (linear-model lm-y lm-x)]
    [lm-x lm]))

(defn schultz-plots [data-dir & {:keys [method] :or {method :int-retention-vol-com}}]
  (let [cs-data (solvent-properties data-dir :solvent_name :cross_sectional_area)
        surface-tension-data (solvent-properties
                              data-dir :solvent_name :dispersive_surface_tension)
        donor-data (solvent-properties data-dir :solvent_name :vOCGelectronDonorParameter)
        acceptor-data (solvent-properties data-dir :solvent_name :vOCGelectronAcceptorParameter)
        data (data-loader data-dir "free-energy" free-energy-cols)
        coverages (vec (into (sorted-set) (sel data :cols :n-nm)))
        solvents (into (sorted-set) (sel data :cols :solvent-name))
        non-polars (vec (sets/difference solvents #{"DICHLOROMETHANE" "ETHYL ACETATE"}))
        ;_ (println coverages)
        ap-sqrts (into {} (map (fn [solvent] (let [ap (cs-data solvent)
                                         gamma (surface-tension-data solvent)]
                                      {solvent (ap-sqrt-gamma ap gamma)}))
                      solvents))
        ;_ (println ap-sqrts)
        data (add-derived-column :RTlnV
                                 [:col-temp method]
                                 RTlnV
                                 data)
        data (add-derived-column :ap-sqrt-gamma
                                 [:solvent-name]
                                 (fn [solvent]
                                   (ap-sqrts solvent))
                                 data)
        non-polar-data ($where ($fn [solvent-name]
                                    (and (not= solvent-name "DICHLOROMETHANE")
                                         (not= solvent-name "ETHYL ACETATE")))
                               data)
        ;non-polar-data data ; use to show polar values
        polar-data ($where ($fn [solvent-name]
                                    (or (= solvent-name "DICHLOROMETHANE")
                                         (= solvent-name "ETHYL ACETATE")))
                               data)
        ;_ (view data)
        slope-data (for [coverage coverages]
                 (let [cov-data ($where ($fn [n-nm] (= n-nm coverage)) non-polar-data)
                       [lm-x lm] (make-linear-model cov-data :ap-sqrt-gamma :RTlnV)
                       [a b] (:coefs lm)
                       r-squared (:r-square lm)
                       _ (println coverage r-squared)]
                   [coverage a b (* 1000.0 b b) r-squared])) ; slope is sqrt gamma (in mJ/m2, so *1000)
        slopes (map #(nth % 3) slope-data)
        r-squares (map #(nth % 4) slope-data)


        gamma-plus (acceptor-data "DICHLOROMETHANE")
        ap-sqrt-gamma-DCM (ap-sqrt-gamma gamma-plus (cs-data "DICHLOROMETHANE"))
        gamma-minus (donor-data "ETHYL ACETATE")
        ap-sqrt-gamma-EA (ap-sqrt-gamma gamma-minus (cs-data "ETHYL ACETATE"))

        _ (println "+" gamma-plus "-" gamma-minus)
        _ (println "ap-sqrt-gamma-DCM" ap-sqrt-gamma-DCM "ap-sqrt-gamma-EA" ap-sqrt-gamma-EA)

        specific-estimates (for [i (range (count coverages))]
                             (let [;_ (println "estimate" i)
                                   [coverage a b slope] (nth slope-data i)
                                   cov-data ($where ($fn [n-nm] (= n-nm coverage)) polar-data)
                                   ;_ (view cov-data)
                                   specific-data (add-derived-column
                                                  :specific-y
                                                  [:ap-sqrt-gamma :RTlnV]
                                                  (fn [x y]
                                                    (- y (+ a (* b x))))
                                                  cov-data)
                                   ;_ (view specific-data)
                                   data-map (into {} ($map (fn [solvent x y] {solvent [x y]})
                                                           [:solvent-name :ap-sqrt-gamma :specific-y]
                                                           specific-data))
                                   [xDCM yDCM] (data-map "DICHLOROMETHANE")
                                   [xEA yEA] (data-map "ETHYL ACETATE")
                                   polar-minus (* avogadro (/ yDCM ap-sqrt-gamma-DCM))
                                   polar-minus-sqr (* 1000.0 polar-minus polar-minus)
                                   polar-plus (* avogadro (/ yEA ap-sqrt-gamma-EA))
                                   polar-plus-sqr (* 1000.0 polar-plus polar-plus)
                                   ;_ (println "y+" yEA "y-" yDCM)
                                   ;_ (println "pol+" polar-plus "pol-" polar-minus)
                                   ;_ (println "gamma+" polar-plus-sqr "gamma-" polar-minus-sqr)

                                   ;_ (println data-map)
                                   ;slope (/ (- y1 y0) (- x1 x0))
                                   ;_ (println coverage slope)
                                   ;[lm-x lm] (make-linear-model specific-data :ap-sqrt-gamma :specific-y)
                                   ;[a b] (:coefs lm)
                                   ]
                               ;(println coverage a b)
                               [coverage [polar-minus polar-plus] [polar-minus-sqr polar-plus-sqr] [xDCM yDCM] [xEA yEA]]))


        _ (count specific-estimates)
        cov0 (first coverages)
        cov0-data ($where ($fn [n-nm] (= n-nm cov0)) non-polar-data)

        plot (xy-plot :ap-sqrt-gamma :RTlnV :data cov0-data
             :legend true
             :x-label "2 * Na * ap * sqrt(gamma)"
             :group-by :n-nm
             :y-label "RTlnV"
             :series-label :n-nm
           :points true)

        lm-plot (xy-plot coverages slopes
             :x-label "Coverage (n/nm)"
             :y-label "Dispersive Surface Energy (mJ/m^2)"
           :points true)

        r-sq-plot (xy-plot coverages r-squares
             :x-label "Coverage (n/nm)"
             :y-label "Coefficient of Determination R^2"
           :points true)
        r-sq-plot (set-plot-theme r-sq-plot :set-min 0.99 :set-max 1.0)

        _ (save-plot r-sq-plot data-dir (str "Dispersive-R-Squared-" (name method)))
        _ (view r-sq-plot :width 720 :height 720)

        show-specific? false
        lm-plot (if show-specific?
                  (add-lines lm-plot coverages (map second specific-estimates)
                           :series-label "Specific Surface Energy"
                           :points true)
                  lm-plot)
        ;lm-plot (set-plot-theme lm-plot)
        lm-plot (set-plot-theme lm-plot :set-min 35.0 :set-max 75.0)
        _ (save-plot lm-plot data-dir (str "Dispersive-Calculated-" (name method)))
        _ (view lm-plot :width 720 :height 720)

        polar-pluss (map #(nth (nth % 1) 1) specific-estimates)
        polar-minuss (map #(nth (nth % 1) 0) specific-estimates)

        polar-plot (xy-plot coverages polar-pluss
             :x-label "Coverage (n/nm)"
             :y-label "Polar Surface Energy (mJ/m^2)"
             :series-label "pol+"
                            :legend true
           :points true)
        polar-plot (add-lines polar-plot coverages polar-minuss
                           :series-label "pol-"
                           :points true)
        _ (save-plot polar-plot data-dir "Polar-Calculated")
        ;_ (view polar-plot :width 720 :height 720)
        plot (reduce
              (fn [p coverage]
                (let [cov-data ($where ($fn [n-nm] (= n-nm coverage)) non-polar-data)]
                  (add-lines p :ap-sqrt-gamma :RTlnV :data cov-data
                         :series-label coverage
                         :points true)))
            plot
            (take-nth 2 (drop 2 coverages)))
        plot (set-plot-theme plot)]
  plot))

(defn bet-data [data-dir amt-col]
  (let [;solvent-data (solvent-properties data-dir :solvent_name :criticalPressure)
        data (data-loader data-dir "injection-items" injection-items-cols)
        data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                     data)
        data ($where ($fn [amount-mmol-g-max amount-mmol-g-com]
                          (and (> amount-mmol-g-max 0)
                               (> amount-mmol-g-com 0))) data)
        data (->> data
                  (add-derived-column :saturationPressure
                                      [:solvent-name :col-temp]
                                      ;[]
                                      ;#(get solvent-data %)
                                      ;#(p0 303.15)
                                      (fn [solvent tK] (p0 (- tK 273.15) solvent))
                                      )
                  (add-derived-column :bet-value
                                      [:partial-pressure :saturationPressure amt-col]
                                      bet-value)
                  (add-derived-column :bet-ordinate
                                      [:partial-pressure :saturationPressure]
                                      bet-ordinate)
                  ($where ($fn [bet-ordinate](and (> bet-ordinate 0)
                                                  (< bet-ordinate 0.6)))))
        ;_ (view data)
        ]
    data))

(defn scientific [x]
  (clojure.string/replace (format "%.3g" x) #"e((\+|-)\d+)" "\\\\times10^{$1}"))

(defn bet-plot
  [data-dir amt-col plot-tag & {:keys [solvent] :or {solvent "OCTANE"}}]
  (let [solvent-data (solvent-properties data-dir :solvent_name :cross_sectional_area)
        data (bet-data data-dir amt-col)
        _ (save-plot-data data data-dir plot-tag)
        plot (plot-maker data injection-items-titles
              :bet-ordinate :bet-value :solvent-name
                (str "BET: " (injection-items-titles amt-col)))
        avogadro 6.0221413e+23
        _ (println "Linear Model..")
        lm-data (->> data
                     ($where ($fn [bet-ordinate solvent-name]
                                  (and (= solvent-name solvent)
                                       (>= bet-ordinate 0.05)
                                       (<= bet-ordinate 0.35)))))
        lm-x (to-matrix (sel lm-data :cols [:bet-ordinate]))
        lm-y (to-matrix (sel lm-data :cols [:bet-value]))
        lm-plot (plot-maker lm-data injection-items-titles
              :bet-ordinate :bet-value :solvent-name
                (str "BET: " (injection-items-titles amt-col)))
        ;_ (view lm-plot)
        ;_ (view lm-data)
        lm (linear-model lm-y lm-x)
        [a b] (:coefs lm)
        _ (println "Adding Linear Model..")
        plot (add-lines plot lm-x (:fitted lm) :series-label "Linear Model" :points true)
        monolayer-capacity (/ 1.0 (+ a b))
        bet-constant (+ 1.0 (/ b a))
        cross-section (get solvent-data solvent)
        specific-area (* monolayer-capacity 0.001 cross-section avogadro)
        lm-str1 (str  "\\mathrm{" solvent ":}\\ "
                      "\\frac{p}{n(p^\\circ-p)}"
                    "="
                    "\\frac{1}{n_\\mathrm{m}c}+"
                    "\\frac{c-1}{n_\\mathrm{m}c}\\frac{p}{p^\\circ}")
        lm-str2 (str "{n_\\mathrm{m}}=" (scientific monolayer-capacity)
                    "\\ \\mathrm{mMol/g},\\ "
                    "c=" (scientific bet-constant) ",\\ "
                    "A_{BET}={n_\\mathrm{m}}{a_\\mathrm{m}}{N_\\mathrm{A}}"
                    "=" (scientific specific-area) "\\ \\mathrm{m}^2\\mathrm{/g}")
        model-str (str "\n\n% Linear Model Info for " data-dir "\n"
                       "% calculated using " amt-col " for " solvent "\n"
                       "\\begin{align*}\n"
                       "&" lm-str1 "\\\\\n"
                       "&" lm-str2
                       "\\\\\n"
                       "&\\mathrm{Linear\\ model\\ fitted:}\\ "
                       "\\frac{p}{n(p^\\circ-p)}=y=a+bx="
                       "\\frac{1}{n_\\mathrm{m}c}+"
                       "\\frac{c-1}{n_\\mathrm{m}c}\\frac{p}{p^\\circ}"
                       ",\\\\\n"
                       "&a=\\frac{1}{n_\\mathrm{m}c}=" (scientific a) ",\\ "
                       "b=\\frac{c-1}{n_\\mathrm{m}c}=" (scientific b) ",\\\\\n"
                       "&\\mathrm{Standard\\ Errors\\ (SER)}\\ \\sigma_a="
                       (scientific (first (:std-errors lm))) ",\\ \\sigma_b="
                       (scientific (second (:std-errors lm))) ",\\\\\n"
                       "&\\mathrm{Coefficient\\ of\\ Determination}\\ "
                       "R^2 = " (scientific (:r-square lm))
                       "\n\\end{align*}")

        caption-str (str "\\caption{(a) Adsorption isotherm for the adsorption of "
                         (s/lower-case solvent) " at XXX K on MoS$_2$ \n"
                         "sample centrifuged at 2000rpm (mixed bulk and nanosheets);\n"
                         "(b) corresponding BET plots. The BET equation was calculated for "
                         (s/lower-case solvent) ", yielding an estimate for "
                         "$A_{BET}=" (scientific specific-area)
                         "\\ \\mathrm{m}^2\\mathrm{/g}$.}\n\n")
        m-name (fs/base-name data-dir)
        figure-str (str "\\begin{figure}[htb]\n"
                "\\subfloat[\\label{pic:sa-iso-" m-name "}]"
                "{\\includegraphics[width=0.5\\textwidth]"
                "{plots/igc/Amt-com-v-Partial-Pressure--" m-name ".png}}\n"
                "\\hfill\n"
                "\\subfloat[\\label{pic:sa-bet-" m-name "}]"
                "{\\includegraphics[width=0.5\\textwidth]"
                "{plots/igc/" plot-tag "--" m-name ".png}}\n"
                caption-str
                "\\label{fig:sa-" m-name "}\n"
                "\\end{figure}")
        lm-str (str lm-str1 ",\\ " lm-str2)
        ;_ (println lm-str)
        tex-str (str model-str "\n\n% Figure for table showing plots.\n\n" figure-str "\n\n\n")
        _ (println tex-str)
        _ (spit (str data-dir "/" plot-tag "--" (fs/base-name data-dir) ".tex") tex-str)
        ;plot (add-latex-subtitle plot lm-str2)
        ;plot (add-latex plot 0.1 (* a 0.6) lm-str1 :background false)
        ;_ (view data)
        ]
    plot))

(defn bet-plots
  [root codes labels ycol & {:keys [solvent hide-titles] :or {solvent "OCTANE" hide-titles true}}]
  (let [dirs (map :dir (vals (select-keys machine-data codes)))
        ;_ (println dirs)
        first-dir (:dir ((first codes) machine-data))
        _ (set-machine-location! (str root "/" first-dir) (machine-files first-dir))
        data-sets (for [code codes]
                    (let [dir (:dir (code machine-data))
                          data-dir (str root "/" dir)
                          fname (fs/base-name data-dir)
                          _ (println "loading " fname)
                          data (bet-data data-dir ycol)
                          ;_ (view data)
                          data ($where ($fn [solvent-name](= solvent-name solvent))
                                       data)
                          data (add-derived-column :dir [] (fn [] (labels code)) data)]
                      data))
        first-data (first data-sets)
        plot (plot-maker first-data injection-items-titles
                         :bet-ordinate :bet-value :dir
                         (str "BET: " (injection-items-titles ycol))
                         :legend true :series-label (labels (first codes)))
        plot (if hide-titles (set-title plot "") plot)
        ;_ (view plot)
        ]
    (reduce (fn [p [data code]]
              (add-lines p :bet-ordinate :bet-value
                         :data data
                         :series-label (labels code)
                         :points true))
            plot
            (map vector (rest data-sets) (rest codes)))))

(defn dispersive-plots [data-dir]
  (let [data (data-loader data-dir "dispersive-surface-energy" dispersive-cols)
        ;_ (view data)
        plot (xy-plot :n-nm :en-stz-com :data data
             :legend true
             :x-label (dispersive-titles :n-nm)
             :y-label "Dispersive Surface Energy (mJ/m^2)"
                      :series-label (dispersive-legend-titles :en-stz-com)
             :title "Dispersive Surface Energy"
           :points true)
        plot (reduce (fn [p col]
                       (add-lines p :n-nm col
                                  :data data
                                  :points true
                                  :series-label (dispersive-legend-titles col)))
                     plot
                     [:en-stz-max :en-dng-com :en-dng-max])
        plot (set-plot-theme plot :set-min 35.0)]
  plot))

(defn dse-plots
  [root codes labels & {:keys [ycol hide-titles] :or {ycol :en-dng-com hide-titles true}}]
  (let [dirs (map :dir (vals (select-keys machine-data codes)))
        ;_ (println dirs)
        first-dir (:dir ((first codes) machine-data))
        _ (set-machine-location! (str root "/" first-dir) (machine-files first-dir))
        data-sets (into {} (for [code codes]
                    (let [dir (:dir (code machine-data))
                          data-dir (str root "/" dir)
                          fname (fs/base-name data-dir)
                          _ (println "loading " fname)
                          data (data-loader data-dir "dispersive-surface-energy" dispersive-cols)
                          ;_ (view data)
                          data (add-derived-column :dir [] (fn [] (labels code)) data)]
                      {code data})))
        #_#_per-coverage (into {}
                           (for
                             [coverage coverages]
                             (let
                               [dse (into
                                     []
                                     (for
                                       [code codes]
                                       (let [data (data-sets code)
                                             row ($where ($fn [n-nm] (= coverage n-nm)) data)
                                             yval ($ ycol row)]
                                         yval)))]
                               {coverage dse})))

        plot (xy-plot
              :n-nm ycol :data (data-sets (first codes))
             :legend true
             :x-label (dispersive-titles :n-nm)
             :y-label (dispersive-titles ycol)
                      :series-label (labels (first codes))
             :title ""
           :points true)
        plot (if hide-titles (set-title plot "") plot)
        plot (set-plot-theme plot)
        ;_ (view plot :width 720 :height 720)
        ]
    (reduce (fn [p code]
              (add-lines p :n-nm ycol :data (data-sets code)
                         :series-label (labels code)
                         :points true))
            plot
            (rest codes))))


(comment
  (def d (bet-data @machine-dir :amount-mmol-g-com))
  (def lm-x (to-matrix (sel d :cols [:bet-ordinate])))
  (def lm-y (to-matrix (sel d :cols [:bet-value])))
  (def lm (linear-model lm-y lm-x))
  )

(defn run-plots [& {:keys [plot-set legend hide-titles] :or
                    {plot-set :surface-area legend false hide-titles true}}]
  (let [m-dir @machine-dir
        m-name (fs/base-name m-dir)
        plot-seq [
                  ]
        extra-plots (if (= plot-set :surface-area)
                      [#_[(bet-plot m-dir :amount-mmol) "BET-mMol"]
                       #_[(bet-plot m-dir :amount-mmol-g) "BET-mMol-g"]
                       [(injection-items-plot m-dir :partial-pressure :net-ret-vol-com
                                              "Volume [com] v Partial Pressure" :legend legend)
                        "Vol-com-v-Partial-Pressure"]
                       [(injection-items-plot m-dir :partial-pressure :net-ret-vol-max
                                              "Volume [max] v Partial Pressure" :legend legend)
                        "Vol-max-v-Partial-Pressure"]
                       [(injection-items-plot m-dir :partial-pressure :amount-mmol-g-max
                                              "Amount [max] v Partial Pressure" :legend legend)
                        "Amt-max-v-Partial-Pressure"]
                       [(injection-items-plot m-dir :partial-pressure :amount-mmol-g-com
                                              "Amount [com] v Partial Pressure" :legend legend)
                        "Amt-com-v-Partial-Pressure"]
                       [(injection-items-plot m-dir :actual-surface-coverage
                                              :net-ret-vol-com
                                              "Volume [com] v Actual Surface Coverage" :legend legend)
                        "Vol-com-v-Actual-Surface-Coverage"]
                       [(injection-items-plot m-dir :actual-surface-coverage
                                              :net-ret-vol-max
                                              "Volume [max] v Actual Surface Coverage" :legend legend)
                        "Vol-max-v-Actual-Surface-Coverage"]
                       [(bet-plot m-dir :amount-mmol-g-com "BET-mMol-g-com") "BET-mMol-g-com"]
                       [(bet-plot m-dir :amount-mmol-g-max "BET-mMol-g-max") "BET-mMol-g-max"]]
                      [[(dispersive-plot m-dir :n-nm :en-stz-max
                                         "Dispersive Energy (Schultz) [max] vs n/n_m")
                        "DSE-Schultz-max"]
                       [(dispersive-plots m-dir)
                        "Dispersive-Surface-Energy"]
                       [(free-energy-plots m-dir)
                        "Free-Energy"]
                       [(schultz-plots m-dir :method :int-retention-vol-com)
                        "Schultz-Plots-com"]
                       [(schultz-plots m-dir :method :int-retention-vol-max)
                        "Schultz-Plots-max"]
                       [(solvents-plot m-dir :partial-pressure :net-ret-vol-com
                                       "Volume [com] v Partial Pressure" :legend legend)
                        "Vol-com-v-Partial-Pressure"]
                       [(solvents-plot m-dir :partial-pressure :net-ret-vol-max
                                       "Volume [max] v Partial Pressure" :legend legend)
                        "Vol-max-v-Partial-Pressure"]
                       [(solvents-plot m-dir :partial-pressure :amount-mmol-g-max
                                       "Amount [max] v Partial Pressure" :legend legend)
                        "Amt-max-v-Partial-Pressure"]
                       [(solvents-plot m-dir :partial-pressure :amount-mmol-g-com
                                       "Amount [com] v Partial Pressure" :legend legend)
                        "Amt-com-v-Partial-Pressure"]
                       [(solvents-plot m-dir :actual-surface-coverage
                                       :net-ret-vol-com
                                       "Volume [com] v Actual Surface Coverage" :legend legend)
                        "Vol-com-v-Actual-Surface-Coverage"]
                       [(solvents-plot m-dir :actual-surface-coverage
                                       :net-ret-vol-max
                                       "Volume [max] v Actual Surface Coverage" :legend legend)
                        "Vol-max-v-Actual-Surface-Coverage"]
                       ])
        plot-seq (reduce conj plot-seq extra-plots)]
    (doseq [[p f] plot-seq]
      (view p :width 720 :height 720)
      (if hide-titles (set-title p ""))
      (save-plot p m-dir f))))

(defn sem-preps
  [root]
  (glob-pattern root "*"))

(defn sem-plot
  [data f]
  (let [ycol :gamma-d
        data (->> data
                  (add-derived-column :total-gamma-plus [:gamma-d :gamma-s-plus] +)
                  (add-derived-column :total-gamma-minus [:total-gamma-plus :gamma-s-minus] +)
                  )
        plot (xy-plot
              :n-nm ycol :data data
              :legend true
              :x-label (sem-titles :n-nm)
              :y-label (sem-titles ycol)
              :series-label (sem-legend-titles ycol)
              :title ""
              :points true)
        hide-titles true
        plot (if hide-titles (set-title plot "") plot)
        plot (set-plot-theme plot)
        ;_ (view plot :width 720 :height 720)
        codes [:gamma-d :gamma-s-plus :gamma-s-minus :gamma-ab :gamma-t]
        ]
    (set-plot-theme (reduce (fn [p code]
              (add-lines p :n-nm code :data data
                         :series-label (sem-legend-titles code)
                         :points true))
            plot
            (rest codes)))))

; (clojure.java.io/reader f :encoding "UTF-16")

(defn load-sem-file
  [f root]
  (let [data (read-dataset f :skip 10 :delim \tab)
        data (rename-cols sem-cols data)
        ;_ (view data)
        plot (sem-plot data f)
        fname (s/replace (fs/base-name f) #".txt" "")
        dir (s/replace (fs/parent f) root "../sem-plots")
        ;_ (view plot :width 720 :height 720)
        save-name (str dir "/" fname ".png")
        _ (println "saving to" save-name)]
    (save plot save-name :width 720 :height 720)))
; n/nm	?d	?s+	?s-	?ab	?t
(defn sem-crawler
  [root & {:keys [prep-pattern sets-pattern file-pattern] :or
                    {prep-pattern "*" sets-pattern "*" file-pattern "*.txt"}}]
  (doall (for [preps (glob-pattern root prep-pattern)
               sets (glob-pattern preps sets-pattern)
               ;data-file (glob-pattern sets "*.txt")
               data-file (glob-pattern sets file-pattern)
               :when (re-matches #".*-p?(com|max).*" (fs/base-name data-file))
               ]
           (load-sem-file data-file root))))


(comment
  (use '[incanter core io stats charts datasets latex])
  (use '[igc surface-energy util] :reload)

  (sem-crawler "../03-Processed-Files" :prep-pattern "Prep-I" :file-pattern "1500*.txt")
  (sem-crawler "../03-Processed-Files" :prep-pattern "Prep-I" :file-pattern "3000*.txt")
  (sem-crawler "../03-Processed-Files" :prep-pattern "Prep-I" :file-pattern "as-received*.txt")

  (sem-crawler "../03-Processed-Files" :prep-pattern "Prep-II" :file-pattern "1500*.txt")

  (set-machine-location! "../Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S1-SA-10ml" "SA-2k-12mg-S1.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S2-SA-10ml" "SA-2k-12mg-S2.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-II-4000rpm-3mg-01-3mm-30C-S1-SA-10ml" "SA-4k-3mg.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-I-3000rpm-8mg-01-3mm-30C-S1-SA-10ml" "SA-3k-8mg.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml" "SA-ref-100mg.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-I-1500rpm-10mg-01-3mm-30C-S1-SA-10ml" "nanosheet-Prep-I-1_5k-10mg-3mm-S1-SA-30C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-I-1500rpm-10mg-01-3mm-30C-S1-SA-10ml-run2" "nanosheet-Prep-I-1_5k-10mg-3mm-S1-SA-30C-run2.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-I-1500rpm-10mg-01-3mm-30C-S2-SA-10ml" "nanosheet-Prep-I-1_5k-10mg-3mm-S2-SA-30C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-I-750rpm-20mg-01-3mm-30C-S1-SA-10ml" "nanosheet-prep-I-750rpm-20mg-3mm-S1-SA-30C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)

  (set-machine-location! "../Nanosheets-Prep-I-750rpm-20mg-01-3mm-30C-S2-SA-10ml" "nanosheet-prep-I-750rpm-20mg-3mm-S2-SA-30C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)
  ; causes problems
  (set-machine-location! "../Reference-As-Supplied-13mg-2mm-100C-S3-SA-10ml" "Reference-13mg-S3-SA-100C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)
  (set-machine-location! "../Reference-As-Supplied-13mg-2mm-30C-S1-SA-10ml" "Reference-13mg-S1-SA-30C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)
  (set-machine-location! "../Reference-As-Supplied-13mg-2mm-30C-S3-SA-10ml" "Reference-13mg-S3-SA-30C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)
  (set-machine-location! "../Reference-As-Supplied-5mg-3mm-30C-S1-SA-10ml" "ref-5mg-SA-30C-S1.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots)
  (set-machine-location! "../Nanosheets-Prep-I-3000rpm-8mg-03-2mm-50C-S1-SE-10ml-redone" "SE-3krpm-8mg.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots :plot-set :surface-energy)

  (set-machine-location! "../Nanosheets-Prep-I-1500rpm-10mg-01-3mm-50C-S1-SE-10ml" "nanosheet-prep-I-1_5k-3mm-S1-SE-50C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots :plot-set :surface-energy)

  (set-machine-location! "../Nanosheets-Prep-I-750rpm-20mg-01-3mm-50C-S2-SE-10ml" "nanosheet-750rpm-3mm-S2-SE-50C.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots :plot-set :surface-energy)

  (set-machine-location! "../Reference-As-Supplied-100mg-3mm-50C-S1-SE-10ml" "SE-ref-100mg.csv")
  (write-se-data-files @machine-file @machine-dir)
  (run-plots :plot-set :surface-energy :legend true)

  (set-machine-location! "../Nanosheets-Prep-II-2000rpm-12mg-01-3mm-50C-S1-SE-10ml" "SE-2krpm-12mg-S1.csv")
  (write-se-data-files @machine-file @machine-dir)
  ; :nano-2-2000-se-s1-12mg:nano-2-2000-se-s1-12mg

  ;(set-machine-location! "../

  (def p (bet-plot @machine-dir :amount-mmol))
  (view p)
  (save-plot p @machine-dir "BET-mmol")
  (def p (bet-plot @machine-dir :amount-mmol-g-max))
  (view p)
  (save-plot p @machine-dir "BET-mmol-g-max")
  (def p (bet-plot @machine-dir :amount-mmol-g-com))
  (view p)
  (save-plot p @machine-dir "BET-mmol-g-com")
  (def p (injection-items-plot @machine-dir :partial-pressure :net-ret-vol-max
                               "Volume [max] vs Partial Pressure"))
  (view p)
  (save-plot p @machine-dir "Volume-max-v-Partial-Pressure")
  (def p (injection-items-plot @machine-dir :partial-pressure :net-ret-vol-com "Volume [com] vs Partial Pressure"))
  (view p)
  (save-plot p @machine-dir "Volume-com-v-Partial-Pressure")
  (def p (injection-items-plot @machine-dir :actual-surface-coverage :net-ret-vol-max "Volume [max] vs Actual Surface Coverage"))
  (view p)
  (save-plot p @machine-dir "Volume-max-v-Actual-Surface-Coverage")
  (def p (injection-items-plot @machine-dir :actual-surface-coverage :net-ret-vol-com
                               "Volume [com] vs Actual Surface Coverage"))
  (view p)
  (save-plot p @machine-dir "Vol-com-vs-Actual-Surface-Coverage")

  (def p (injection-plots ".." codes sa-labels :partial-pressure :amount-mmol-g-com "Amount (com) vs Partial Pressure"))
  (save-plot p "../igc" "Nanosheets-Amt-com-vs-Partial-Pressure.png")
  (view p)
  (def p (injection-plots ".." codes :partial-pressure :amount-mmol-g-max "Amount (max) vs Partial Pressure"))
  (view p)
  (save-plot p "../igc" "Nanosheets-Amt-max-vs-Partial-Pressure.png")
  (def p (injection-plots ".." codes :actual-surface-coverage :net-ret-vol-com "Retention Vol (com) vs Coverage"))
  (view p)
  (save-plot p "../igc" "Nanosheets-Ret-Vol-com-vs-Coverage.png")


  (def p (bet-plots ".." sa-codes sa-labels :amount-mmol-g-com))
  (view p)
  (save-plot p "../igc" "Nanosheets-BET-Amt-com.png")

  ;

  ; Vapor pressure (P sat) by the Antoine Equation: ln P sat/kPa = A − B
;t/◦C+C
;Latent heat of vaporization at the normal boiling point (!Hn), and normal boiling point (tn)
;Parameters for Antoine Eqn. Temp. Range !Hn tn
;Name Formula A† B C ◦C kJ/mol ◦C
; n-Octane C8H18 13.9346 3123.13 209.635 26 — 152 34.41 125.6
; Dichloromethane CH2Cl2 13.9891 2463.93 223.240 −38 — 60 28.06 39.7
; Diethyl ether C4H10O 14.0735 2511.29 231.200 −43 — 55 26.52 34.4
; n-Heptane C7H16 13.8622 2910.26 216.432 4 — 123 31.77 98.4
; n-Hexane C6H14 13.8193 2696.04 224.317 −19 — 92 28.85 68.7
; n-Nonane C9H20 13.9854 3311.19 202.694 46 — 178 36.91 150.8
  )

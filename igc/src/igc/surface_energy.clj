(ns igc.surface-energy
  (:use [incanter core io stats charts datasets latex])
  (:require [me.raynes.fs :as fs]
            [clojure.string :as s]
            [igc.util :refer :all])
  (:gen-class :main true))


(def machine-file-list
  [
   [:code :ref-1-supplied-sa-s1-100mg
    :dir "Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml"
    :machine-file "SA-ref-100mg.csv"]
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
   [:code :nano-2-4000-sa-s1-3mg
    :dir "Nanosheets-Prep-II-4000rpm-3mg-01-3mm-30C-S1-SA-10ml"
    :machine-file "SA-4k-3mg.csv"]
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
                             :actual-surface-coverage "Actual Fractional Surface Coverage"
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

(defn plot-maker
  [data title-map x-col y-col grp-col title & {:keys [legend] :or {legend false}}]
  (xy-plot x-col y-col :data data
             :group-by grp-col :legend legend
             :x-label (title-map x-col)
             :y-label (title-map y-col)
             :title title
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
        ;_ (view data)
        ]
  (plot-maker data injection-items-titles
              xcol ycol :solvent-name title :legend legend)))

(defn injection-plots
  [root codes xcol ycol title & {:keys [solvent] :or {solvent "OCTANE"}}]
  (let [dirs (map :dir (vals (select-keys machine-data codes)))
        _ (println dirs)
        first-dir (:dir ((first codes) machine-data))
        _ (set-machine-location! (str root "/" first-dir) (machine-files first-dir))
        data-sets (for [dir dirs]
                    (let [data-dir (str root "/" dir)
                          fname (fs/base-name data-dir)
                          _ (println "loading " fname)
                          data (data-loader data-dir "injection-items" injection-items-cols)
                          data ($where ($fn [injection-name](re-matches #"injection.*" injection-name))
                                   data)
                          data ($where ($fn [solvent-name](= solvent-name solvent))
                                   data)
                          data (add-derived-column :dir [] (fn [] fname) data)]
                      [fname data]))
        first-data (first data-sets)
        plot (plot-maker (second first-data) injection-items-titles
              xcol ycol :dir title :legend true :series-label (first codes))]
    (reduce (fn [p data-tuple]
                       (add-lines p xcol ycol
                                  :data (second data-tuple)
                                  :series-label (first data-tuple)
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
             :y-label (dispersive-titles :en-dng-com)
             :title "Dispersive Surface Energy"
           :points true)
        plot (reduce (fn [p col]
                       (add-lines p :n-nm col
                                  :data data
                                  :points true
                                  :series-label (dispersive-titles col)))
                     plot
                     [:en-dng-max :en-stz-com :en-stz-max])]
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
        plot (add-lines plot lm-x (:fitted lm) :series-label "Linear Model")
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
        plot (add-latex-subtitle plot lm-str2)
        plot (add-latex plot 0.1 (* a 0.6) lm-str1 :background false)
        ;_ (view data)
        ]
    plot))

(defn bet-plots
  [root codes ycol & {:keys [solvent] :or {solvent "OCTANE"}}]
  (let [dirs (map :dir (vals (select-keys machine-data codes)))
        ;_ (println dirs)
        first-dir (:dir ((first codes) machine-data))
        _ (set-machine-location! (str root "/" first-dir) (machine-files first-dir))
        data-sets (for [dir dirs]
                    (let [data-dir (str root "/" dir)
                          fname (fs/base-name data-dir)
                          _ (println "loading " fname)
                          data (bet-data data-dir ycol)
                          ;_ (view data)
                          data ($where ($fn [solvent-name](= solvent-name solvent))
                                   data)
                          data (add-derived-column :dir [] (fn [] fname) data)]
                      data))
        first-data (first data-sets)
        plot (plot-maker first-data injection-items-titles
              :bet-ordinate :bet-value :dir
                (str "BET: " (injection-items-titles ycol))
                    :legend true :series-label (first codes))
        ;_ (view plot)
        ]
    (reduce (fn [p [data code]]
                       (add-lines p :bet-ordinate :bet-value
                                  :data data
                                  :series-label code
                                  :points true))
                     plot
            (map vector (rest data-sets) (rest codes)))))


(comment
  (def d (bet-data @machine-dir :amount-mmol-g-com))
  (def lm-x (to-matrix (sel d :cols [:bet-ordinate])))
  (def lm-y (to-matrix (sel d :cols [:bet-value])))
  (def lm (linear-model lm-y lm-x))
  )

(defn run-plots [& {:keys [plot-set legend] :or {plot-set :surface-area legend false}}]
  (let [m-dir @machine-dir
        m-name (fs/base-name m-dir)
        plot-seq [[(injection-items-plot m-dir :partial-pressure :net-ret-vol-max
                               "Volume [max] v Partial Pressure" :legend legend)
                  "Vol-max-v-Partial-Pressure"]
                   [(injection-items-plot m-dir :partial-pressure :net-ret-vol-com
                               "Volume [com] v Partial Pressure" :legend legend)
                  "Vol-com-v-Partial-Pressure"]
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

               ]
        extra-plots (if (= plot-set :surface-area)
                      [#_[(bet-plot m-dir :amount-mmol) "BET-mMol"]
                           #_[(bet-plot m-dir :amount-mmol-g) "BET-mMol-g"]
                           [(bet-plot m-dir :amount-mmol-g-com "BET-mMol-g-com") "BET-mMol-g-com"]
                           [(bet-plot m-dir :amount-mmol-g-max "BET-mMol-g-max") "BET-mMol-g-max"]]
                      [[(dispersive-plot m-dir :n-nm :en-stz-max
                               "Dispersive Energy (Schultz) [max] vs n/n_m")
                               "DSE-Schultz-max"]
                       [(dispersive-plots m-dir)
                               "Dispersive-Surface-Energy"]
                       ])
        plot-seq (reduce conj plot-seq extra-plots)]
    (doseq [[p f] plot-seq]
    (view p)
    (save-plot p m-dir f))))

(comment
  (use '[incanter core io stats charts datasets latex])
  (use '[igc surface-energy util] :reload)
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

  (def codes [:nano-1-750-sa-s1-20mg
              :nano-1-750-sa-s2-20mg
              :nano-1-1500-sa-s2-10mg
              :nano-2-2000-sa-s1-12mg
              :nano-2-2000-sa-s2-12mg
              :nano-1-3000-sa-s1-8mg
              :nano-2-4000-sa-s1-3mg]
    )
  (def p (injection-plots ".." codes :partial-pressure :amount-mmol-g-com "Amount (com) vs Partial Pressure"))
  (save-plot p "../igc" "Nanosheets-Amt-com-vs-Partial-Pressure.png")
  (view p)
  (def p (injection-plots ".." codes :partial-pressure :amount-mmol-g-max "Amount (max) vs Partial Pressure"))
  (view p)
  (save-plot p "../igc" "Nanosheets-Amt-max-vs-Partial-Pressure.png")
  (def p (injection-plots ".." codes :actual-surface-coverage :net-ret-vol-com "Retention Vol (com) vs Coverage"))
  (view p)
  (save-plot p "../igc" "Nanosheets-Ret-Vol-com-vs-Coverage.png")

    (def sa-codes [:nano-1-750-sa-s1-20mg
              :nano-1-750-sa-s2-20mg
              :nano-1-1500-sa-s2-10mg
              :nano-2-2000-sa-s1-12mg
              :nano-2-2000-sa-s2-12mg
              :nano-1-3000-sa-s1-8mg
              :nano-2-4000-sa-s1-3mg]
    )
  (def p (bet-plots ".." sa-codes :amount-mmol-g-com))
  (view p)
  (save-plot p "../igc" "Nanosheets-BET-Amt-com.png")

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

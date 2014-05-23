(ns igc.core
  (:use [incanter core io stats charts datasets])
  (:require [me.raynes.fs :as fs]
            [clojure.string :as s])
  (:gen-class :main true))

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

(def datasets (for [substance substances
                    treatment (glob-pattern substance "*")
                    sample-size (glob-pattern treatment "*")
                    measurement (glob-pattern sample-size "*")]
                (let [measurement-vector
                      (->> [substance treatment sample-size measurement]
                           (map fs/base-name)
                           vec)
                      substance-name (clean-file-name substance #"^\d+-" "")
                      treatment-name (clean-file-name treatment #"^\d+-" "")
                      sample-description (clean-file-name sample-size #"^0+" "")
                      column-description (clean-file-name measurement #"^\d+-" "")
                      [col-size temp sample-id meas flow] (clojure.string/split column-description #" ")]
                  [substance-name treatment-name sample-description
                   col-size temp sample-id meas flow])))

(def rows
  (clojure.string/join "\\\\\n"
       (for [line datasets] (clojure.string/join " & " line))))

(def table
  (str "
 \\begin{table}[htb]
 \\begin{tabular}{l l c c c c c c l}
 \\toprule
 Substance & Treatment  & Sample  & Column & T & Sample # & Measuring & Flow & $\\varnothing$ & Comments \\\\
 & & &(mg)&(mm)&($^\\circ$C)& & &(ml/min)&(mm) &\\\\
 \\midrule
 "
 rows
 "
 \\bottomrule
 \\end{tabular}
\\caption{IGC surface area measurement conditions on MoS$_2$ reference powder (Sigma Aldrich).}
\\end{table}"))

(def root-dir (fs/file ".."))

(def igc-runs (glob-pattern root-dir "Nanosheets*"))

(def igc-data (for [igc-run igc-runs
                    export (glob-pattern igc-run "export")
                    index-csv (glob-pattern export "masterSchedule.csv")
                    injections-csv (glob-pattern export "masterInjection.csv")]
                (let [csv-files (vec (glob-pattern export "*.csv"))
                      injections (vec (glob-pattern export "injection*.csv"))
                      run-description (fs/base-name igc-run)]
                  {:run run-description
                   :run-dir igc-run
                   :schedule index-csv
                   :csv-files csv-files
                   :injections injections
                   :injection-settings injections-csv})))

(defn injection-config [inj-file sched-file]
  (let [injection-data (read-dataset inj-file :header true)
        schedule-data (read-dataset sched-file :header true)
        schedule-data ($where ($fn [injection_name](re-matches #"injection.*" injection_name))
                              schedule-data)
        injection-map (into {}
                            ($map (fn [col gain] {col gain})
                                  [:injection_table_name :FIDGain] injection-data))
        solvent-map (into {}
                            ($map (fn [col solvent] {col solvent})
                                  [:injection_name :solvent] schedule-data))]
    [injection-map solvent-map]))

(defn table-map [files injections-csv sched-file]
	(let [[injection-map solvent-map] (injection-config injections-csv sched-file)]
       (into {} (for [f files]
  	   (let [data ($where {:ID {:$lt 2001}} (read-dataset f :header true))
             fname (s/replace (fs/base-name f) #".csv$" "")
             gain (injection-map fname)
             solvent (solvent-map fname)
             normalised (add-derived-column :normalised [:fid_signal]
                                            #(/ % gain) data)
	           with-name (tag-data normalised :injection fname)
             with-name (tag-data with-name :solvent solvent)
             _ (println "Loaded" fname (length (:rows with-name)) "rows.. Gain " gain
                        (col-names with-name) solvent)
]
	    {fname with-name})))))

(defn combined-table [files injections-csv sched-file]
	(let [data-map (table-map files injections-csv sched-file)
        tables (vals data-map)]
    (reduce conj-rows (first tables) (rest tables))))

(def i1 (nth (:injections (second igc-data)) 4))
(def inj1 (:injection-settings (second igc-data)))
(def sched1 (:schedule (second igc-data)))
(def t1 (read-dataset i1 :header true))
(def test-injs (take 200 (sort (:injections (second igc-data)))))

(def solvents (second (injection-config inj1 sched1)))
(def solvent-names (into #{} (vals solvents)))

(def plot-sets (into {} (for [solvent solvent-names]
             (let [injections (filter #(= solvent (second %)) solvents)]
               {solvent (map first injections)}))))

;(def test-t ($order :normalised :desc (combined-table test-injs inj1)))
(println "Combining tables..")
(def test-t (combined-table test-injs inj1 sched1))
(println "Combined tables.")

;(def sampled-t (sel test-t :filter (fn [x] (zero? (rand-int 10)))))
;(def sampled-t test-t)
(println "Plotting..")

(def plot-tables (doall (into {} (for [solvent solvent-names]
             (let [_ (println "Extracting" solvent)
                   solvent-data ($where {:solvent solvent} test-t)
                   _ (println "Extracted" solvent (length (:rows solvent-data)) "rows..")]
               {solvent solvent-data})))))

(defn plot-solvent
  [solvent]
  (let [data (plot-tables solvent)]
    (println "Plotting" solvent (length (:rows data)) "rows..")
    (xy-plot :sea_time :normalised :data data
             :group-by :injection :legend false
             :title solvent)))

(comment
(def sc1 (scatter-plot :sea_time :fid_signal :data t1))
(def sc2 (xy-plot :sea_time :normalised :data sampled-t :group-by :injection :legend false))
  )


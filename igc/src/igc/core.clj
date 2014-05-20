(ns igc.core
  (:use [incanter core io stats charts datasets])
  (:require [me.raynes.fs :as fs])
  (:gen-class :main true))

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
      (clojure.string/replace pattern repl)
      (clojure.string/replace "-" " ")))

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



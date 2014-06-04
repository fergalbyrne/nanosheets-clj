(ns igc.util)

(def machine-dir (atom "../Reference-As-Supplied-100mg-3mm-30C-S1-SA-10ml"))
(def machine-file (atom (str @machine-dir "/SA-ref-100mg.csv")))

(defn set-machine-location!
  [d f]
  (reset! machine-dir d)
  (reset! machine-file (str @machine-dir "/" f)))

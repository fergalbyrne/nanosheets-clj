nanosheets-clj
==============

Using Clojure to analyse physicochemical properties of MoS2 nanosheets.

This repo contains software developed to work on a Diplom thesis currently being
completed by Louise Klodt on the preparation of Molybdenum Disulphide nanosheets.

The Clojure code is in the igc folder, other folders contain data and products of the software and related analysis.

## Usage

From igc/ run 

    igc$ lein repl

### Surface Area Plots

    ; load the code
    (use 'igc.surface-energy :reload)
    ;
    ; provide the directory name and machine file locations
    (set-machine-location! "../Nanosheets-Prep-II-2000rpm-12mg-01-3mm-30C-S1-SA-10ml" "SA-2k-12mg-S1.csv")
    ;
    ; create the four csv files from the machine file
    (write-se-data-files @machine-file @machine-dir)
    ;
    ; create and save the plots
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


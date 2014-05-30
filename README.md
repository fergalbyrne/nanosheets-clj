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
    (run-plots)


# Wrapper script for the java RRunner to call.
# Parses a json file containing parameters and assigns
# them to global variables. Note that this is pretty hacky
# and we should instead put them into a dictionary in global scope.
# This script takes two mandatory command-line arguments:
# --paramfile, and --rscript, which are the json file containing the
# parameters, and the rscript to be executed, respectively.

library("optparse")
cmd_args <- commandArgs(TRUE);
option_list <- list(
    make_option("--paramfile"),
    make_option("--inputpath"),
    make_option("--outputpath"),
    make_option("--rscript")
)
args <- parse_args(OptionParser(option_list = option_list), args = c(cmd_args))

library("rjson")
tzar <- fromJSON(paste(readLines(args$paramfile, warn=FALSE), collapse=""))
inputpath = args$inputpath
outputpath = args$outputpath

inputFiles <- lapply(tzar$inputFiles, function(x) { paste(inputpath, x, sep='') } )
outputFiles <- lapply(tzar$outputFiles, function(x) { paste(outputpath, x, sep='') } )
variables <- tzar$variables

source(args$rscript)

# for debugging: prints out the parameters
# str(tzar)

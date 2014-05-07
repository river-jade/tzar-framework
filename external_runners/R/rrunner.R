# Wrapper script for the java RRunner to call.
# Parses a json file containing parameters and assigns
# them to global variables. Note that this is pretty hacky
# and we should instead put them into a dictionary in global scope.
# This script takes two mandatory command-line arguments:
# --paramfile, and --rscript, which are the json file containing the
# parameters, and the rscript to be executed, respectively.


# Define a function to check if a package is installed and perfrom a
# personal install if it is not (this function uses a personal install to avoid
# issue with not having permissions for a system install).


install.if.required <- function(pkg) {
    if (!pkg %in% installed.packages()) {
        cat( "\n *** Note: package", pkg,
            "required by rrunner.R is not installed. Trying to install now." )
        local.lib.path <- Sys.getenv("R_LIBS_USER") # This is the default place for a personal install
        if( !file.exists(local.lib.path) ) dir.create(local.lib.path, recursive=TRUE)
        cat("\n *** Installing packages to", local.lib.path, "\n\n" )
        install.packages(pkg, lib=local.lib.path, dependencies=TRUE, repos='http://cran.r-project.org')
    }
}

   #--------------------------------------------------------------
   # Install packages required by rrunner.R (if required)
   #--------------------------------------------------------------

install.if.required( "optparse" )
install.if.required( "rjson" )

# Add the default location of the personal install directory to the libPaths
.libPaths(Sys.getenv("R_LIBS_USER"))


   #--------------------------------------------------------------
   # Parse the command-line arguments 
   #--------------------------------------------------------------

library("optparse")
cmd_args <- commandArgs(TRUE)
option_list <- list(
    make_option("--paramfile"),
    make_option("--inputpath"),
    make_option("--outputpath"),
    make_option("--rscript")
)
args <- parse_args(OptionParser(option_list = option_list), args = c(cmd_args))


   #--------------------------------------------------------------
   # Parse the json file containg project info (variables, output  
   # paths etc) to an object called "tzar" 
   #--------------------------------------------------------------

library("rjson")
tzar <- fromJSON(paste(readLines(args$paramfile, warn=FALSE), collapse=""))
inputpath = args$inputpath
outputpath = args$outputpath

# Make an object called "parameters" containing all the variables in the json file
# in the R script, variables can then be accessed via parameters$PARAMETER.NAME in the R script
parameters <- tzar$parameters

# for debugging: prints out the parameters
# str(tzar)

   #--------------------------------------------------------------
   # Source the project's R script
   #--------------------------------------------------------------

tryCatch( 
    
    source(args$rscript),
    warning = function(warning)  cat( " *** WARNING in rrunner.R:", warning ), 
    error = function(error) cat( " *** ERROR in rrunner.R:", error ), 
    finally = {

          #--------------------------------------------------------------
          # Write info to the tzar output dir irrespective of if the
          # script succeeds or fails
          #--------------------------------------------------------------

        # Write a version of the parameters to a file that can be sourced directly in R 
        dump( c('parameters'), paste( outputpath, '/parameters.R', sep='') )

        # Dump the output of R's sessionInfo() command to a file in the output dir
        si <- paste( outputpath, '/R_sessionInfo.txt', sep='')
        writeLines(capture.output(date(), cat("\n"), sessionInfo()), con=si)
    }

)

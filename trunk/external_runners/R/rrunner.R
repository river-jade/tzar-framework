# Wrapper script for the java RRunner to call.
# Parses a json file containing parameters and assigns
# them to global variables. Note that this is pretty hacky
# and we should instead put them into a dictionary in global scope.
# This script takes two mandatory command-line arguments:
# --paramfile, and --rscript, which are the json file containing the
# parameters, and the rscript to be executed, respectively.

#-------------------------------------------------------------------------------
#
#  History: 
#
#   - Created by River Satya.
#
#   - Modified by Ascelin Gordon
#
#   - 2015 02 13 - Modified by Bill Langford
#       - Fixed the way the tryCatch handles errors so that it will 
#         properly print error messages when there is a crash, plus it 
#         will print the traceback of the stack.
#       - Before this change, an error would produce the same odd error 
#         message no matter what the error was.  The message would say 
#         something about cat not being able to print a list.  This was 
#         due to a cat statement in the error branch of the tryCatch 
#         trying to print the error condition object, which appears to be 
#         a list.  The "print" statement can do this but "cat" can't do it.
#         The fix here completely changes the way all that is handled 
#         because it was useful to get the traceback, etc.
#
#-------------------------------------------------------------------------------

    #--------------------------------------------------------------------------
    # Define a function to check if a package is installed and perfrom a
    # personal install if it is not (this function uses a personal install to 
    # avoid issue with not having permissions for a system install).
    #--------------------------------------------------------------------------


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

# If new libraries we added to the R_LIBS_USER location (the personal
# install directory), then add this location of to the .libPaths,
# which tells R where to look for libraries
if( file.exists( Sys.getenv("R_LIBS_USER") ) ) .libPaths( c(.libPaths(), Sys.getenv("R_LIBS_USER")) )


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

    #--------------------------------------------------------------------------
    #  Creating a global flag to indicate whether an error has been trapped 
    #  in the RRunner.  Tried to make the name unique enough not to be 
    #  accidentally interfering with a variable name anywhere else in the 
    #  code.  
    #
    #  Have not found a way to get the traceback and correct handling of 
    #  error from inside the tryCatch without using a global variable. 
    #
    #  Also setting the default error handler to get the traceback since 
    #  it doesn't seem possible to get the traceback from inside the tryCatch 
    #  (see comments in tryCatch below for more detail).
    #--------------------------------------------------------------------------

failed_in____RRunner = FALSE
options(error = 
            function() 
                { 
                message ("\n\n-----  FATAL ERROR IN R CODE  -----: \n"); 
                traceback(2); 
                    #  VERY IMPORTANT THAT THIS IS A GLOBAL ASSIGNMENT
                failed_in____RRunner <<- TRUE    
                }
        )

   #--------------------------------------------------------------
   # Source the project's R script
   #--------------------------------------------------------------

failed_in____RRunner = tryCatch( 
    {
    failed_in____RRunner = FALSE
    source (args$rscript)
    },
        #  BTL - 2015 02 13
        #  Removed both the warning and the error branches of this tryCatch.
        #  The warning part was unnecessary since R will spit those out 
        #  automatically.
        #  The error part had to be handled by the global "options(error = " 
        #  statement to get the traceback included in the error output.  
        #  For some reason, traceback is not accessible inside a tryCatch: 
        #  the R help for traceback says:
        #      "Errors which are caught via try or tryCatch do not generate 
        #       a traceback, so what is printed is the call sequence for the 
        #       last uncaught error, and not necessarily for the last error."

    finally = {

          #--------------------------------------------------------------
          # Write info to the tzar output dir irrespective of if the
          # script succeeds or fails
          #--------------------------------------------------------------

        # Write a version of the parameters to a file that can be sourced directly in R 
        dump( c('parameters'), paste( outputpath, '/metadata/parameters.R', sep='') )

        # Dump the output of R's sessionInfo() command to a file in the output dir
        si <- paste( outputpath, '/metadata/R_sessionInfo.txt', sep='')
        writeLines(capture.output(date(), cat("\n"), sessionInfo()), con=si)
    }
)

if (failed_in____RRunner) 
    {
        #  Setting the return status to 10 to match the R help for the 
        #  q() function's recommendation about return status.
    
    q (save = "no", status = 10, runLast = FALSE)
    }


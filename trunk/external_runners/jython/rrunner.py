import os
import subprocess
import sys
import traceback

from au.edu.rmit.tzar.api import Runner

class RRunner(Runner):
    def __init__(self, rpath, inputpath, rtermlocation = None, dryrun = False):
        self.rpath = rpath
        self.dryrun = dryrun
        self.inputpath = inputpath

        self.rtermlocation = rtermlocation if rtermlocation else "Rscript"

    def runModel(self, modelpath, outputpath, runid, flags, params, logger):
        self.write_run_params_to_r(params, outputpath)
        cmdarray = [self.rtermlocation] + flags
        cmdstring = ' '.join(cmdarray)
        if self.dryrun:
            logger.fine("Would have run R command: %s" % cmdstring)
            return

        logger.fine("Running R command: %s" % cmdstring)

        cmd = subprocess.Popen(cmdarray, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, cwd=self.rpath)
        # Loop over stdout and log. Without the weird looking iter() etc,
        # this blocks until EOF. There may be a nicer way to do this.
        for line in iter(cmd.stdout.readline, ''):
            logger.fine(line.rstrip())
        returncode = cmd.wait()

        if returncode != 0:
            raise Exception("Error executing R code '%s'. Rscript exited with "\
                            "returncode = %s." % (cmdstring, returncode))

        logger.fine("Finished running R code '%s'" %  cmdstring)

    def write_run_params_to_r(self, params, outputpath):
        routfile = open(os.path.join(self.rpath, "variables.R"), 'w')

        for key, val in dict(params.asMap()).items():
            if type(val) == bool:
                val = str(val).upper()
            elif type(val) == unicode or type(val) == str:
                val = "\"" + val.replace("\\", "\\\\") + "\""
            routfile.write("%s <- %s\n" % (key, val))
        routfile.close()

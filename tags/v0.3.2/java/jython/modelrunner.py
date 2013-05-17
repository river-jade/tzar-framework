#!/usr/bin/jython

import datetime
import optparse
import os
import shlex
import sys
import traceback

import rrunner

import java.io.File

from org.python.core import PyException

from au.edu.rmit.tzar.api import Runner

class ModelRunner(Runner):
    def runModel(self, modelpath, outputpath, runid, flags, params, logger):
        try:
            parser = optparse.OptionParser()
            options = self.parse_flags(parser, flags)
            if not options.projectname or not options.inputdir:
                parser.print_help()
                print # needed to flush the IOBuffer
                return False

            projectname = options.projectname.strip()
            inputpath = java.io.File(java.io.File(modelpath, "projects/" + projectname),
                    options.inputdir.strip())
            runner = rrunner.RRunner(rtermlocation=options.rlocation.strip(), rpath=os.path.join(modelpath.toString(), "R"),
                    inputpath=inputpath, dryrun=options.dryrun)
            modelmodule = __import__("projects.%s.model" % projectname, fromlist=["Model"])
            model = modelmodule.Model(runner, inputpath, outputpath, runid, logger)
            start = datetime.datetime.now()

            logger.fine('='*60)
            logger.fine("Executing run: %s" % runid)
            logger.fine("Outputting temporary files to %s" % outputpath)
            logger.fine('='*60)

            model.execute(params)

            td = datetime.datetime.now() - start
            logger.fine("Run took %s min(s) %s second(s)" % (td.seconds / 60, td.seconds % 60))
        except SystemExit, e:
            logger.warning("SystemExit was called:\n" + traceback.format_exc())
            return False
        except Exception, e:
            logger.warning("An error occurred executing the model:\n" + traceback.format_exc())
            return False
        return True # success!

    def get_decimal_params(self, params):
        """Convert all BigDecimal values into decimals, because otherwise
        multiplication fails when run in jython (because
        decimals get passed as java.math.BigDecimal, which can't be used with '*')
        """
        return dict((k, decimal.Decimal(str(v)) if type(v) is java.math.BigDecimal else v) for k, v in
            dict(params.getVariables()).iteritems())

    def parse_flags(self, parser, flags):
        """Configures the command-line flag parser.
        """
        parser.add_option('-d', "--dryrun", action="store_true", dest="dryrun", 
                          default=False, help="If set, R code won't be executed")
        parser.add_option("--inputdir", action="store", dest="inputdir", 
                          default="input_data",
                          help="Relative path for input data")
        parser.add_option('-p', "--projectname", action="store", dest="projectname",
                          help="Name of project to process")
        parser.add_option("--repetitionsfile", action="store",
                          dest="repetitionsfile",
                          help="Filename containing repetition definitions")
        parser.add_option("--rlocation", action="store",
                          dest="rlocation",
                          help="Command to run Rscript.",
                          default="Rscript")
        parser.add_option("--seed", action="store", dest="seed",
                          help="Random number seed")

        options, args = parser.parse_args(shlex.split(flags))
        return options

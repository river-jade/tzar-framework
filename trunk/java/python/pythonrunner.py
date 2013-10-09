#!/usr/bin/python

import datetime
import json
import logging
import optparse
import os
import sys
import traceback

from model import Model

def main(args):
    logger = Logger(logging.getLogger())
    logger.addHandler(logging.StreamHandler(sys.stdout))
    logger.setLevel(logging.DEBUG)

    try:
        parser = optparse.OptionParser()

        options = parse_flags(parser, args)

        if not options.modelpath:
            parser.print_help()
            print # needed to flush the IOBuffer
            return 2

        modelpath = options.modelpath
        outputpath = options.outputpath
        runid = options.runid
        inputpath = os.path.join(modelpath, options.inputdir)

        params = Parameters(json.load(file(options.paramfile)))

        sys.path.insert(0, modelpath)

        model = Model(inputpath, outputpath, runid, logger)
        start = datetime.datetime.now()

        logger.debug('='*60)
        logger.debug("Executing run: %s" % runid)
        logger.debug("Outputting temporary files to %s" % outputpath)
        logger.debug('='*60)
        model.execute(params)

        td = datetime.datetime.now() - start
        logger.debug("Run took %s min(s) %s second(s)" % (td.seconds / 60, td.seconds % 60))
    except SystemExit, e:
        logger.exception("SystemExit was called.")
        return 1
    except Exception, e:
        logger.exception("An error occurred executing the model.")
        return 1
    else:
        return 0 # success!


def parse_flags(parser, flags):
    """Configures the command-line flag parser.
    """

    parser.add_option('-d', "--dryrun", action="store_true", dest="dryrun",
                      default=False, help="If set, R code won't be executed")
    parser.add_option("--inputdir", action="store", dest="inputdir",
                      default="input_data",
                      help="Relative path for input data")
    parser.add_option("--modelpath", action="store", dest="modelpath",
                      help="Path to the model code.")
    parser.add_option("--outputpath", action="store", dest="outputpath",
                      help="Path to write the output data to.")
    parser.add_option("--paramfile", action="store", dest="paramfile", help="Path to the parameters json file")
    parser.add_option("--runid", action="store", dest="runid",
                      help="ID of the run to execute.")
    parser.add_option("--seed", action="store", dest="seed",
                      help="Random number seed")

    options, args = parser.parse_args(flags)

    return options

class Logger(object):
    def __init__(self, logger):
        self.logger = logger

    def debug(self, arg):
        self.logger.debug(arg)

    def info(self, arg):
        self.logger.info(arg)

    def warning(self, arg):
        self.logger.warning(arg)

    def fine(self, arg):
        self.logger.debug(arg)

    def exception(self, arg):
        self.logger.exception(arg)

    def addHandler(self, handler):
        self.logger.addHandler(handler)

    def setLevel(self, level):
        self.logger.setLevel(level)

class Parameters(object):
    def __init__(self, json):
        self.variables = json['variables']
        self.inputfiles = json['inputFiles']
        self.outputfiles = json['outputFiles']

    def getVariables(self):
        return self.variables

    def getInputFiles(self):
        return self.inputfiles

    def getOutputFiles(self):
        return self.outputfiles

    def get_size(self):
        return len(outputfiles) + len(inputfiles) + len(variables)

    def getQualifiedParams(self, baseInputPath, baseOutputPath):
        ret = {}
        ret.update(self.variables)
        ret.update(dict([(x, os.path.join(baseInputPath, y)) for (x, y) in self.inputfiles.iteritems()]))
        ret.update(dict([(x, os.path.join(baseOutputPath, y)) for (x, y) in self.outputfiles.iteritems()]))
        return ret

    def merge_parameters(self, overrideParameters):
        Pass
        
    def merge_parameters(variables, inputFiles, outputFiles):
        Pass

if __name__ == "__main__":
    sys.exit(main(sys.argv))
#!/bin/python

import optparse
import os
import shutil
import sys

def main(argv=None):
    if argv is None:
        argv = sys.argv
    parser = optparse.OptionParser()
    parser.add_option("--run_ids", dest="runids", 
        help="Comma separated list of run ids whose results are to be copied, "
                "or '*' to copy all runs.")
    parser.add_option("--outputdir", dest="outputdir", 
        help="Directory to copy the aggregated results to.")
    try:
        opts, args = parser.parse_args()
        if not opts.runids:
            raise Usage("--run_ids is required.")
        if not opts.outputdir:
            raise Usage("--outputdir is required.")
        copyruns(opts.runids.split(','), opts.outputdir)
    except Usage, err:
        print err.msg
        parser.print_help()
        return 2

def copyruns(runids, outputdir):
    sourcepath = os.path.expanduser("~/tzar/outputdata")
    dirs = os.listdir(sourcepath)

    # filter out 'in progress' output dirs
    dirs = [os.path.join(sourcepath, name) for name in dirs if not name.endswith('.inprogress')]

    # extract run id
    runs = [(name, name.split('_')[-1]) for name in dirs]

    # walk the output tree for each run, copying and renaming the files therein
    for run in runs:
        runpath = run[0]
        runid = run[1]
        if runid in runids or runids[0] == '*':
            print "Copying run: %s" % runid
            # small hack to effectively curry the copydir function with the output dir
            copy = lambda runid, dirname, names: copydir(outputdir, runid, dirname, names)
            os.path.walk(runpath, copy, runid)

def copydir(outputdir, runid, dirname, names):
    for name in names:
        source = os.path.join(dirname, name)
        if not os.path.isdir(source):
            shutil.copy2(source, os.path.join(outputdir, runid + '_' + name))

class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

if __name__ == "__main__":
    sys.exit(main())

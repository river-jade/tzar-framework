import decimal
import os
import glob

import java.math.BigDecimal

class BaseModel(object):
    """Base class for models."""

    def __init__(self, rrunner, inputpath, outputpath, runid, logger=None):
        self.rrunner = rrunner
        self.inputpath = inputpath
        self.outputpath = outputpath
        self.runid = runid
        self.logger = logger

    def execute(self, params):
        """Execute the model code with the parameters supplied."""
        pass

    def get_decimal_params(self, params):
        """Convert all BigDecimal values into decimals, because otherwise
        multiplication fails when run in jython (because
        decimals get passed as java.math.BigDecimal, which can't be used with '*')
        """
        return dict((k, decimal.Decimal(str(v)) if type(v) is java.math.BigDecimal else v) for k, v in
            dict(params.getVariables()).iteritems())

    def run_r_code(self, rscript, params, variables=None, inputfiles=None,
                   outputfiles=None):
        myparams = params
        if (variables or inputfiles or outputfiles):
            if not variables: variables = {}
            if not inputfiles: inputfiles = {}
            if not outputfiles: outputfiles = {}
            myparams = params.mergeParameters(variables, inputfiles, outputfiles)
        flags = [rscript]
        self.rrunner.runModel(None, self.outputpath, self.runid, flags, myparams, self.logger)

import decimal
import os
import glob

class BaseModel(object):
    """Base class for models."""

    def __init__(self, inputpath, outputpath, runid, logger=None):
        self.inputpath = inputpath
        self.outputpath = outputpath
        self.runid = runid
        self.logger = logger

    def execute(self, params):
        """Execute the model code with the parameters supplied."""
        pass

    def get_decimal_params(self, params):
        """This is a no-op for this implementation. It's in the interface for the Jython
        implementation. See ../jython/basemodel.py for details.
        """
        return params.variables

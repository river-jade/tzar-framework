import csv
import glob
import os
import sys

import basemodel

class Model(basemodel.BaseModel):
    def execute(self, runparams):
        """Executes the model and writes the output to the CSV file specified in the project params.
        """
        # get parameters qualified by input and output file paths
        qp = runparams.getQualifiedParams(self.inputpath, self.outputpath)

        # Log the variable values
        self.logger.fine("random.seed: %s" % qp["random.seed"])
        self.logger.fine("test.variable.1: %s" % qp["test.variable.1"])
        self.logger.fine("test.variable.2: %s" % qp["test.variable.2"])
        self.logger.fine("test.variable.3: %s" % qp["test.variable.3"])
        self.logger.fine("test.variable.4: %s" % qp["test.variable.4"])

        # write some variables to output files
        with open(qp["output.filename"], 'w') as f1:
            f1.write(str(qp["test.variable.1"]) + "\n")

        with open(qp["output.filename2"], 'w') as f2:
            f2.write(str(qp["test.variable.2"]) + "\n")



import basemodel

class Model(basemodel.BaseModel):
    def execute(self, runparams):
        """Executes the model and writes the output to the CSV file specified in the project params.
        """
        # get parameters qualified by input and output file paths
        params = runparams['parameters']

        # Log the variable values
        self.logger.fine("random.seed: %s" % params["random.seed"])
        self.logger.fine("test.variable.1: %s" % params["test.variable.1"])
        self.logger.fine("test.variable.2: %s" % params["test.variable.2"])
        self.logger.fine("test.variable.3: %s" % params["test.variable.3"])
        self.logger.fine("test.variable.4: %s" % params["test.variable.4"])
        self.logger.fine("test.variable.5: %s" % params["test.variable.5"])
        self.logger.fine("test.variable.5 size: %s" % len(params["test.variable.5"]))
        self.logger.fine("test.variable.5 type: %s" % type(params["test.variable.5"]))
        self.logger.fine("test.variable.6: %s" % params["test.variable.6"])

        # write some variables to output files
        with open(params["output.filename"], 'w') as f1:
            f1.write(str(params["test.variable.1"]) + "\n")

        with open(params["output.filename2"], 'w') as f2:
            f2.write(str(params["test.variable.2"]) + "\n")



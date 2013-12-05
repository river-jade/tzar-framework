import basemodel

class Model(basemodel.BaseModel):
    def execute(self, runparams):
        # the logging levels are (in descending order): severe, warning, info, config, fine,
        # finer, finest
        # by default tzar will log all at info and above to the console, and all logging to a logfile.
        # if the --verbose flag is specified, all logging will also go to the console.
        self.logger.fine("I'm in model.py!!")

        # gets the variables, with (java) decimal values converted to python decimals
        # this is useful if you want to use arithmetic operations within python.
        variables = self.get_decimal_params(runparams)

        self.logger.fine("\nRandom.seed is %s" % variables['random.seed'])
        self.logger.fine("\nTest variable 4 is the string: %s" % variables['test.variable.4'])
        self.logger.fine("\nPAR.testing.output.filename is: %s" % variables['PAR.testing.output.filename'])
        self.logger.fine("\nPAR.testing.output.filename2 is: %s" % variables['PAR.testing.output.filename2'])

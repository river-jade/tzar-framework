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
        params = self.get_decimal_params(runparams)

        self.logger.fine("\nRandom.seed is %s" % params['random.seed'])
        self.logger.fine("\nTest variable 4 is the string: %s" % params['test.variable.4'])
        self.logger.fine("\noutput.filename is: %s" % params['output.filename'])
        self.logger.fine("\noutput.filename2 is: %s" % params['output.filename2'])

        # write some params to output files
        f1 = open(params["output.filename"], 'w')
        f1.write(str(params["test.variable.1"]) + "\n")
        f1.close()

        f2 = open(params["output.filename2"], 'w')
        f2.write(str(params["test.variable.2"]) + "\n")
        f2.close()


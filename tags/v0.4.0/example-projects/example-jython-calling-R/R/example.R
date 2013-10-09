source( 'variables.R' )

# R file for testing the output during dry runs

cat('In test.for.python.dry.run.R...\n')

print('')
for (i in 1:20) {
  cat('A sample output line: ', i, '\n') 
  Sys.sleep(.005)
} 

cat('Value for test variable 1 is:', test.variable.1, '\n')
cat('Value for test variable 2 is:', test.variable.2, '\n')
cat('Value for test variable 3 is:', test.variable.3, '\n')
cat('Value for test variable 4 is:', test.variable.4, '\n')

cat('The working dir is', getwd(), '\n')
cat('PAR.testing.output.filename=', PAR.testing.output.filename, '\n')


test.text <- rep(1:10) 
write.table(test.text, PAR.testing.output.filename )


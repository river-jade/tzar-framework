# R file for testing the output during dry runs

# an example of sourcing another R doc
source( "w.R" )

for (i in 1:20) {
  cat('A sample output line: ', i, '\n') 
  Sys.sleep(.005)
} 

x <- matrix( nrow=20, ncol=20)
x[] <- 1:(20*20) 


cat('Value for test variable 1 is:', parameters$'test.variable.1', '\n')
cat('Value for test variable 2 is:', parameters$'test.variable.2', '\n')
cat('Value for test variable 3 is:', parameters$'test.variable.3', '\n')
cat('Value for test variable 4 is:', parameters$'test.variable.4', '\n')

cat('The working dir is', getwd(), '\n')
cat('PAR.testing.output.filename=', parameters$'PAR.testing.output.filename', '\n')

cat('\n\n##The current working dir is', getwd(), '\n\n' ) 

test.text <- rep(1:10) 

write.table(test.text, parameters$'PAR.testing.output.filename' )
write.table(x, parameters$'PAR.testing.output.filename2' )
str(parameters$'PAR.testing.output.filename')

str(parameters)

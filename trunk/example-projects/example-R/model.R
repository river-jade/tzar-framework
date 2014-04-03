# R file for testing the output during dry runs

# an example of sourcing another R doc
source( "w.R" )

# an example of sourcing another R doc from a library
source( parameters$'path.to.example.script' )

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

cat('\nValue for test variable 5 is:', parameters$'test.variable.5', '\n')
cat('Length of inline array test variable 5 is:', length(parameters$'test.variable.5'), '\n')
cat('Is test variable 5 a vector?:', is.vector(parameters$'test.variable.5'), '\n')
cat('Is test variable 5 numeric?:', is.numeric(parameters$'test.variable.5'), '\n')
cat('Is test variable 5 character?:', is.character(parameters$'test.variable.5'), '\n')

cat('\nValue for indented array test variable 6 is:', parameters$'test.variable.6', '\n')
cat('Length of test variable 6 is:', length(parameters$'test.variable.6'), '\n')
cat('Is test variable 6 a vector?:', is.vector(parameters$'test.variable.6'), '\n')
cat('Is test variable 6 numeric?:', is.numeric(parameters$'test.variable.6'), '\n')
cat('Is test variable 6 character?:', is.character(parameters$'test.variable.6'), '\n')

cat('\nValue for indented string array test variable 7 is:', parameters$'test.variable.7', '\n')
cat('Length of test variable 7 is:', length(parameters$'test.variable.7'), '\n')
cat('Is test variable 7 a vector?:', is.vector(parameters$'test.variable.7'), '\n')
cat('Is test variable 7 numeric?:', is.numeric(parameters$'test.variable.7'), '\n')
cat('Is test variable 7 character?:', is.character(parameters$'test.variable.7'), '\n')

cat('\nValue for inline string array test variable 8 is:', parameters$'test.variable.8', '\n')
cat('Length of test variable 8 is:', length(parameters$'test.variable.8'), '\n')
cat('Is test variable 8 a vector?:', is.vector(parameters$'test.variable.8'), '\n')
cat('Is test variable 8 numeric?:', is.numeric(parameters$'test.variable.8'), '\n')
cat('Is test variable 8 character?:', is.character(parameters$'test.variable.8'), '\n')

cat('\nValue for inline string array test variable 9 is:', parameters$'test.variable.9', '\n')
cat('Length of test variable 9 is:', length(parameters$'test.variable.9'), '\n')
cat('Is test variable 9 a vector?:', is.vector(parameters$'test.variable.9'), '\n')
cat('Is test variable 9 numeric?:', is.numeric(parameters$'test.variable.9'), '\n')
cat('Is test variable 9 character?:', is.character(parameters$'test.variable.9'), '\n')

cat('\nValue for inline string array test variable 10 is:', parameters$'test.variable.10', '\n')
cat('Length of test variable 10 is:', length(parameters$'test.variable.10'), '\n')
cat('Is test variable 10 a vector?:', is.vector(parameters$'test.variable.10'), '\n')
cat('Is test variable 10 numeric?:', is.numeric(parameters$'test.variable.10'), '\n')
cat('Is test variable 10 character?:', is.character(parameters$'test.variable.10'), '\n')

cat('\nThe working dir is', getwd(), '\n')
cat('test.output.filename=', parameters$'test.output.filename', '\n')

test.text <- rep(1:10)

write.table(test.text, parameters$'test.output.filename' )
write.table(x, parameters$'test.output.filename2' )
str(parameters$'test.output.filename')

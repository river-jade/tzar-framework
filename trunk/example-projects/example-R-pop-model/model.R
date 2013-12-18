# R file for testing the output during dry runs


# first set the random seed to the runID so that the results can be replicated
set.seed(parameters$run.id)


# Define matrix to store the repetitions of the pop trajectories (also for log of population size)
pop.traj <- matrix( ncol=parameters$no.timesteps+1, nrow=parameters$reps )
pop.traj.log <- matrix( ncol=parameters$no.timesteps+1, nrow=parameters$reps )

# Set the initial population size 
pop.traj[,1] <- parameters$init.pop.size
pop.traj.log[,1] <- log(parameters$init.pop.size)


# Generate the repetitions
for( r in 1:parameters$reps ) {

    # generate the error terms to be used for each time step
    eta <- rnorm(parameters$no.timesteps, mean=0, sd=parameters$sd )

    for( t in 1:(parameters$no.timesteps) ) {

        # Calculate the time population values for each time step for
        # the log and linear versions of the mode
        pop.traj[r, t+1] <- pop.traj[r,t] * exp(parameters$mu) * exp( eta[t] )
        pop.traj.log[r, t+1] <- pop.traj.log[r,t] + parameters$mu + eta[t]

    }

}



       #------------------------
       #  Calculate statistics
       #-------------------------

# calculate the mean of the pop trajectories
mean.traj.log <- apply( pop.traj.log, 2, mean)
mean.traj <- apply( pop.traj, 2, mean)

# calculate the median of the pop trajectories
median.traj.log <- apply( pop.traj.log, 2, median)
median.traj <- apply( pop.traj, 2, median)

# calculate the min of the pop trajectories
min.traj.log <- apply( pop.traj.log, 2, min)
min.traj <- apply( pop.traj, 2, min )

# calculate the variance of the pop trajectories
var.traj.log <- apply( pop.traj.log, 2, var)
var.traj <- apply( pop.traj, 2, var)

#cat('\n***mean traj = \n' )
#print( mean.traj.log )



       #------------------------
       #  Write output files
       #-------------------------

cat( '\nRun id =', parameters$run.id )
cat( "\noutput file is", parameters$output.plot, '\n' )
cat( "\noutput R dump file is", parameters$output.R.dump.file, '\n' )

# write a graph of a single realisation (the 1st)
pdf( paste( parameters$output.plot, '.single.pdf', sep='' ) )
plot( 0:parameters$no.timesteps, pop.traj.log[1,], type='l', ylim=c(2,8),
     xlab="Time (years)", ylab="Log of population size" )
dev.off()


# now make a plot showing all realisations
pdf( paste(parameters$output.plot, 'multiple.pdf',sep='') )
for( r in 1:parameters$reps) {
    if( r==1 ) plot( 0:parameters$no.timesteps, pop.traj.log[r,], type='l', ylim=c(0,8),
            xlab="Time (years)", ylab="Log of population size" )
    else lines (0:parameters$no.timesteps, pop.traj.log[r,], type='l' )
}
# add the mean line to the plot
lines(0:parameters$no.timesteps, mean.traj.log, type='l', col='red', lwd=4  )

# plot the version without the log transformation
for( r in 1:parameters$reps) {
    if( r==1 ) plot( 0:parameters$no.timesteps, pop.traj[r,], type='l', ylim=c(0,1200),
            xlab="Time (years)", ylab="Population size" )
    else lines (0:parameters$no.timesteps, pop.traj[r,], type='l')
}
# plot the mean line
lines(0:parameters$no.timesteps, mean.traj, type='l', col='red', lwd=4  )

dev.off()

# Write the full matrix with all the population trajectories as a R object for later analysis. 
dump( "pop.traj", parameters$output.R.dump.file )


# Write summary statistics to a file in a single line that can easily
# be aggregated by tzar over multiple runs

column.names <- c('runId', 'initPopSize', 'mu', 'sd', 'reps', 'median', 'medianLog', 'var',
                  'varLog', 'min', 'minLog')

line.to.paste <- c(
    parameters$run.id,
    parameters$init.pop.size,
    parameters$mu,
    parameters$sd,
    parameters$reps,
    round(median.traj[parameters$timeToEvalPopStats],2),
    round(median.traj.log[parameters$timeToEvalPopStats],2),
    round(var.traj[parameters$timeToEvalPopStats],2),
    round(var.traj.log[parameters$timeToEvalPopStats],2),
    round(min.traj[parameters$timeToEvalPopStats],2),
    round(min.traj.log[parameters$timeToEvalPopStats],2)
    )


cat( column.names, '\n', file=parameters$output.statistics.file)
cat( line.to.paste, file=parameters$output.statistics.file, append=TRUE)


#glob.output.file <- paste( parameters$global.output.dir, '/', parameters$global.output.filename, sep='')
#if(  !file.exists(glob.output.file ) ) {
#    cat( column.names, '\n', file=glob.output.file, append=TRUE)
#}
#cat( line.to.paste, '\n', file=glob.output.file, append=TRUE)

cat ("\n\n run no = ", parameters$run.id )

## pdf(parameters$output.plot.pdf)
## plot( 0:50, pop.traj, type='l' )
## dev.off()

#c(parameters$run.id, mean.traj.log, 

#dev.copy( png, parameters$output.plot); dev.off()

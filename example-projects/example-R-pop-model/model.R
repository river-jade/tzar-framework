# R file for testing the output during dry runs

# define matrix to store the pop trajs 

pop.traj <- matrix( ncol=parameters$no.timesteps+1, nrow=parameters$reps )
pop.traj[,1] <- parameters$init.pop.size

pop.traj.log <- matrix( ncol=parameters$no.timesteps+1, nrow=parameters$reps )
pop.traj.log[,1] <- log(parameters$init.pop.size)


for( r in 1:parameters$reps ) {
    
    eta <- rnorm(parameters$no.timesteps, mean=0, sd=parameters$sd )
    for( t in 1:(parameters$no.timesteps) ) {

        pop.traj[r, t+1] <- pop.traj[r,t] * exp(parameters$mu) * exp( eta[t] )
        pop.traj.log[r, t+1] <- pop.traj.log[r,t] + parameters$mu + eta[t]
        #browser()
    }

}



       #------------------------
       #  Calculate statistics
       #-------------------------

# calculate the mean pop trajectories
mean.traj.log <- apply( pop.traj.log, 2, mean)
mean.traj <- apply( pop.traj, 2, mean)

median.traj.log <- apply( pop.traj.log, 2, median)
median.traj <- apply( pop.traj, 2, median)

min.traj.log <- apply( pop.traj.log, 2, min)
min.traj <- apply( pop.traj, 2, min )


var.traj.log <- apply( pop.traj.log, 2, var)
var.traj <- apply( pop.traj, 2, var)

#cat('\n***mean traj = \n' )
#print( mean.traj.log )



       #------------------------
       #  Write output files
       #-------------------------

cat( '\nRun id =', parameters$run.id )
cat( "\noutput file is", parameters$output.plot, '\n' )
cat( "\noutput R dump file is", parameters$output.dump, '\n' )

# write a graph of a single relisatin (the 1st)
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
# plot the mean line
lines(0:parameters$no.timesteps, mean.traj.log, type='l', col='red', lwd=4  )

# show the version without the log transformation
for( r in 1:parameters$reps) {
    if( r==1 ) plot( 0:parameters$no.timesteps, pop.traj[r,], type='l', ylim=c(0,1200),
            xlab="Time (years)", ylab="Population size" )
    else lines (0:parameters$no.timesteps, pop.traj[r,], type='l')
}
# plot the mean line
lines(0:parameters$no.timesteps, mean.traj, type='l', col='red', lwd=4  )

dev.off()

# Dumpt all the matrix with all the poptrajectories. 
dump( "pop.traj", parameters$output.dump )



glob.output.file <- paste( parameters$global.output.dir, '/', parameters$global.output.filename, sep='')


column.names <- c('runId', 'initPopSize', 'mu', 'sd', 'reps', 'median', 'medianLog', 'var', 'varLog',
                  'min', 'minLog')

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

if(  !file.exists(glob.output.file ) ) {
    cat( column.names, '\n', file=glob.output.file, append=TRUE)
}
cat( line.to.paste, '\n', file=glob.output.file, append=TRUE)



## pdf(parameters$output.plot.pdf)
## plot( 0:50, pop.traj, type='l' )
## dev.off()

#c(parameters$run.id, mean.traj.log, 

#dev.copy( png, parameters$output.plot); dev.off()

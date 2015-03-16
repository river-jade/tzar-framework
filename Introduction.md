# Introduction #

Tzar is a framework for running computer simulation models in a way which is reproducible, scalable, and flexible. Its primary goal is to address the issue of reproducibility in modelling science, by allowing researchers to rerun simulations with the same code and sets of parameters at some point in the future.

Tzar's core is written in Java, and can be deployed on any platform that has a recent (>= v1.6) installation of the Java Virtual Machine. Tzar works seamlessly as a local application, which is very convenient while developing and testing models, as well as for projects which don't require large scale computing resources.

It also scales seamlessly up to local "ad-hoc" clusters which can be easily created using either old machines, or utilising unused compute resources (say desktop machines that are unused overnight). Tzar can also be deployed on large-scale dedicated clusters, as long as the hosts on these clusters can run Java, and communicate over the network.

For more detailed documentation, go to http://tzar-framework.atlassian.net/wiki/display/TD
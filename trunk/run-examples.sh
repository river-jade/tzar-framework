#!/bin/bash

for project in example-projects/*; do 
  java -jar out/tzar.jar execlocalruns $project
  retval=$?
  if [ $retval != 0 ]; then
    echo "Example project $project failed";
    exit $retval
  fi
done
echo "All example projects ran successfully."

#!/bin/bash
TZAR_HOME=${TZAR_HOME:-$HOME/tzar}

path=$(find $TZAR_HOME/outputdata -name "$1_*")/metadata/parameters.R

echo "source(\"$path\")"


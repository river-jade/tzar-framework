#!/bin/sh

export PYTHONPATH=$HOME/usr/opt/pyyaml/lib/python2.6/site-packages
python -c '
import sys, json, yaml
json.dump(yaml.load(sys.stdin), sys.stdout, indent=2)
print
' $@


#!/bin/sh

export PYTHONPATH=$HOME/usr/opt/pyyaml/lib/python2.6/site-packages
python -c '
import sys, json, yaml
print yaml.safe_dump(yaml.load(sys.stdin), default_flow_style=False)
' $@



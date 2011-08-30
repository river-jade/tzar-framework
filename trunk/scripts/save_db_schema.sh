#!/bin/bash

pg_dump -s -f ../db/db_schema.sql rdv -U e86020 -W -h arcs-db.vpac.org.au -p 5433

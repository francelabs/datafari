#!/bin/bash -e
#
#
# Startup script for Datafari
#
#



../../../cassandra/bin/cqlsh -f ../../../datafari-cassandra/conf/dev-env/tables
../../../cassandra/bin/cqlsh -f create-admin-dev.txt

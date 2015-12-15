#!/bin/bash -e
#
#
# Configuration of the Cassandra database and creation of the user admin
#
#




../../../cassandra/bin/cqlsh -f ../../../cassandra/conf/dev-env/tables
../../../cassandra/bin/cqlsh -f create-admin-dev.txt


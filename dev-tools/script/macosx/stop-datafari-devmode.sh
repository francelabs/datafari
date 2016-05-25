#!/bin/bash -e
#
#
# Shutdown script for Datafari
#
#



DIR=../../../macosx/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"

if is_running $ZK_PID_FILE; then
   bash $ZK_HOME/bin/zkServer.sh stop
else
   echo "Warn : Zookeeper does not seem to be running."
fi

if is_running $SOLR_PID_FILE; then
   SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr stop
else
   echo "Warn : Solr does not seem to be running."
fi

if is_running $CASSANDRA_PID_FILE; then
   kill $(cat $CASSANDRA_PID_FILE)
   rm $CASSANDRA_PID_FILE
else
   echo "Warn : Cassandra does not seem to be running."
fi

